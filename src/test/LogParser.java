package test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import main.client.ClientState;
import main.protocol.Domain;
import main.protocol.ProtocolCommand;
import main.protocol.Request;
import main.protocol.RequestDeserializer;
import main.server.Annonce;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class LogParser {

    private class LogEvent {
        private String info;
        private String content;

        public LogEvent(String info, String content) {
            this.info = info;
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public String getInfo() {
            return info;
        }
    }

    public static boolean equalHashMapRequest(ProtocolCommand c, Map<String, Object> map1, Map<String, Object> map2) {
        return switch (c) {
            case REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV -> Arrays.equals((byte[]) map1.get("PublicKey"), (byte[]) map2.get("PublicKey")) && Arrays.equals((byte[]) map1.get("IV"), (byte[]) map2.get("IV"));
            case REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK -> Arrays.equals((byte[]) map1.get("PublicKey"), (byte[]) map2.get("PublicKey"));
            case SIGN_UP -> map1.get("Mail").equals(map2.get("Mail")) && map1.get("Name").equals(map2.get("Name")) && map1.get("Pwd").equals(map2.get("Pwd"));
            case SIGN_UP_OK -> map1.get("Mail").equals(map2.get("Mail")) && map1.get("Name").equals(map2.get("Name"));
            case SIGN_IN -> map1.get("Mail").equals(map2.get("Mail")) && map1.get("Pwd").equals(map2.get("Pwd")) && map1.get("SendDomainList") == map2.get("SendDomainList");
            case SIGN_IN_OK -> map1.get("Name").equals(map2.get("Name"));
            case DOMAINS_LIST_OK -> Arrays.equals((Domain[]) map1.get("Domains"), (Domain[]) map2.get("Domains"));
            case ANNONCE_FROM_DOMAIN -> map1.get("Domain").equals(map2.get("Domain"));
            case ANNONCE_FROM_DOMAIN_OK -> Arrays.equals((Annonce[]) map1.get("AnnoncesFromDomain"), (Annonce[]) map2.get("AnnoncesFromDomain"));
            case CREATE_ANNONCE -> map1.get("Title").equals(map2.get("Title")) && map1.get("Descriptif").equals(map2.get("Descriptif")) && map1.get("Domain").equals(map2.get("Domain")) && map1.get("Price").equals(map2.get("Price"));
            case CREATE_ANNONCE_OK -> map1.get("Title").equals(map2.get("Title"));
            case UPDATE_ANNONCE -> (int) map1.get("Price") == (int) map2.get("Price") && (int) map1.get("Id") == (int) map2.get("Id") && map1.get("Title").equals(map2.get("Title")) && map1.get("Descriptif").equals(map2.get("Descriptif"));
            case REMOVE_ANNONCE -> (int) map1.get("Id") == (int) map2.get("Id");
            case SIGN_OUT, DOMAINS_LIST, SIGN_OUT_OK, UPDATE_ANNONCE_OK, REMOVE_ANNONCE_OK -> true;
            case REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_KO, SIGN_UP_KO, SIGN_IN_KO, CREATE_ANNONCE_KO, SIGN_OUT_KO, REMOVE_ANNONCE_KO, ANNONCE_FROM_DOMAIN_KO, UPDATE_ANNONCE_KO, DOMAINS_LIST_KO -> map1.get("Error").equals(map2.get("Error"));
        };
    }

    public boolean checkLog(String log, Object... reqs) {
        try{
            FileInputStream fstream = new FileInputStream(log);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line1, line2;
            int index = 0;
            ArrayList<LogEvent> logEvents = new ArrayList<>();
            while((line1 = br.readLine()) != null && (line2 = br.readLine()) != null) {
                LogEvent event = new LogEvent(line1, line2);
                logEvents.add(event);
            }
            if (logEvents.size() != reqs.length) {
                System.out.println("Expected: " + reqs.length);
                System.out.println("Current : " + logEvents.size());
            }
            assert logEvents.size() == reqs.length;
            for (LogEvent event: logEvents) {
                if (event.getContent().contains("INFO: [STATUS] ")) {
                    if (reqs[index] != ClientState.valueOf(event.getContent().substring(15))) {
                        System.out.println("Expected [" + index + "]: " + reqs[index]);
                        System.out.println("Current  [" + index + "]: " + event.getContent().substring(15));
                    }
                    assert reqs[index] == ClientState.valueOf(event.getContent().substring(15));
                    index++;
                } else if (event.getContent().contains("INFO: [INTERNAL] ")) {
                    if (!reqs[index].equals(event.getContent().substring(6))) {
                        System.out.println("Expected [" + index + "]: " + reqs[index]);
                        System.out.println("Current  [" + index + "]: " + event.getContent().substring(6));
                    }
                    assert reqs[index].equals(event.getContent().substring(6));
                    index++;
                } else if (event.getContent().contains("INFO: [SEND] ") || event.getContent().contains("INFO: [RECEIVE] ")) {
                    String extract_request = "";
                    if (event.getContent().contains("INFO: [SEND] "))
                        extract_request = event.getContent().substring(13);
                    else
                        extract_request = event.getContent().substring(16);
                    Gson gson = new GsonBuilder().registerTypeAdapter(Request.class, new RequestDeserializer()).create();
                    Request request = gson.fromJson(extract_request, Request.class);
                    if (((Request) reqs[index]).getCommand() != request.getCommand()) {
                        System.out.println("Expected [" + index + "]: " + ((Request) reqs[index]).getCommand());
                        System.out.println("Current  [" + index + "]: " + request.getCommand());
                    }
                    assert ((Request) reqs[index]).getCommand() == request.getCommand();
                    // assert ((Request) reqs[index]).getParams() == null || equalHashMapRequest(((Request) reqs[index]).getCommand(), ((Request) reqs[index]).getParams(), request.getParams());
                    if (!equalHashMapRequest(((Request) reqs[index]).getCommand(), ((Request) reqs[index]).getParams(), request.getParams())) {
                        System.out.println("Expected [" + index + "]: " + ((Request) reqs[index]).getParams());
                        System.out.println("Current  [" + index + "]: " + request.getParams());
                    }
                    assert equalHashMapRequest(((Request) reqs[index]).getCommand(), ((Request) reqs[index]).getParams(), request.getParams());
                    index++;
                }
            }
            fstream.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        return true;
    }
}
