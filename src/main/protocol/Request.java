package main.protocol;


import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class Request {

    public ProtocolCommand command;
    public Map<String, Object> param;

    public Request (ProtocolCommand command, Object... params) {
        this.command = command;
        this.param = new HashMap<>();
        switch (command) {
            case REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV -> {
                this.param.put("PublicKey", params[0]);
                this.param.put("IV", params[1]);
            }
            case REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK -> {
                this.param.put("PublicKey", params[0]);
            }
            case SIGN_UP -> {
                this.param.put("Mail", params[0]);
                this.param.put("Name", params[1]);
                this.param.put("Pwd", params[2]);
            }
            case SIGN_UP_OK -> {
                this.param.put("Mail", params[0]);
                this.param.put("Name", params[1]);
            }
            case SIGN_IN -> {
                this.param.put("Mail", params[0]);
                this.param.put("Pwd", params[1]);
                this.param.put("SendDomainList", params[2]);
            }
            case SIGN_IN_OK -> this.param.put("Name", params[0]);
            case SIGN_IN_KO, SIGN_UP_KO, SIGN_OUT_KO, DOMAINS_LIST_KO, ANNONCE_FROM_DOMAIN_KO, REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_KO, CREATE_ANNONCE_KO, UPDATE_ANNONCE_KO, REMOVE_ANNONCE_KO -> {
                this.param.put("Error", params[0]);
            }
            case CREATE_ANNONCE -> {
                this.param.put("Domain", params[0]);
                this.param.put("Title", params[1]);
                this.param.put("Descriptif", params[2]);
                this.param.put("Price", params[3]);
            }
            case CREATE_ANNONCE_OK -> {
                this.param.put("Title", params[0]);
            }
            case UPDATE_ANNONCE -> {
                this.param.put("Title", params[0]);
                this.param.put("Descriptif", params[1]);
                this.param.put("Price", params[2]);
                this.param.put("Id", params[3]);
            }
            case DOMAINS_LIST_OK -> {
                this.param.put("Domains", params[0]);
            }
            case ANNONCE_FROM_DOMAIN -> this.param.put("Domain", params[0]);
            case ANNONCE_FROM_DOMAIN_OK -> this.param.put("AnnoncesFromDomain", params[0]);
            case REMOVE_ANNONCE -> this.param.put("Id", params[0]);
            case DOMAINS_LIST, UPDATE_ANNONCE_OK, REMOVE_ANNONCE_OK, SIGN_OUT_OK, SIGN_OUT -> {}
        }
    }

    public ProtocolCommand getCommand() {
        return command;
    }

    public Map<String, Object> getParams() {
        return this.param;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
