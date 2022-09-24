package main.server;

import com.google.gson.*;
import main.protocol.*;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class ClientHandler implements Runnable, ClientToServerResponseProtocol {
    private Socket socket = null;
    private DataInputStream dis = null;
    private DataOutputStream dos = null;
    private Server server = null;
    private SecretKey sk = null;
    private byte[] iv;
    private boolean clientLogged = false;
    private boolean stopReceiveCommand = false;
    private int id;
    private String mail;
    //private Logger logger;
    //private FileHandler fh;

    public ClientHandler(Socket socket, Server server, int id) throws IOException {
        this.socket = socket;
        this.dis = new DataInputStream(this.socket.getInputStream());
        this.dos = new DataOutputStream(this.socket.getOutputStream());
        this.server = server;
        this.id = id;
        /*
        this.logger = Logger.getLogger("LogServer");
        fh = new FileHandler("LogServer.log");
        this.logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        this.logger.setUseParentHandlers(false); // do not print on console

        this.logger.info("New client handler connected with socket: " + this.socket);
        */
        //this.server.getLogger().info("[INTERNAL] New client handler with id " + this.id);
        this.server.getLogger().info(new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, this.id).toString());
    }

    public String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public String encodeHexString(byte[] byteArray) {
        StringBuilder hexStringBuffer = new StringBuilder();
        for (byte b : byteArray) {
            hexStringBuffer.append(byteToHex(b));
        }
        return hexStringBuffer.toString();
    }

    private void performKeyAgreement(Request req) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException {
        byte[] otherPk = (byte[]) req.getParams().get("PublicKey");
        KeyFactory kf = KeyFactory.getInstance("EC");
        X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(otherPk);
        PublicKey otherPublicKey = kf.generatePublic(pkSpec);

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(this.server.getPrivate());
        ka.doPhase(otherPublicKey, true);
        byte[] sharedSecret = ka.generateSecret();

        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        hash.update(sharedSecret);
        List<ByteBuffer> keys = Arrays.asList(ByteBuffer.wrap(this.server.getPk().getEncoded()), ByteBuffer.wrap(otherPk));
        Collections.sort(keys);
        hash.update(keys.get(0));
        hash.update(keys.get(1));

        byte[] derivedKey = hash.digest();
        this.sk = this.getKeyFromSecret(encodeHexString(derivedKey), "key");
    }

    @Override
    public void run() {
        while (!this.stopReceiveCommand) {
            try {
                Request req = receiveRequest();
                if (req == null) {
                    this.stopReceiveCommand = true;
                    continue;
                }
                processInput(req);
            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException |
                     ClassNotFoundException | InvalidAlgorithmParameterException | NoSuchPaddingException |
                     IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void processInput(Request req) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        switch (req.getCommand()) {
            case REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV -> this.responseToRequestPublicKey(req);
            case SIGN_UP -> this.responseToSignUp(req);
            case SIGN_IN -> this.responseToSignIn(req);
            case SIGN_OUT -> this.responseToSignOut(req);
            case CREATE_ANNONCE -> this.responseToCreateAnnonce(req);
            case UPDATE_ANNONCE -> this.responseToUpdateAnnonce(req);
            case ANNONCE_FROM_DOMAIN -> this.responseAnnonceFromDomain(req);
            case DOMAINS_LIST -> this.responseToDomainList();
            case REMOVE_ANNONCE -> this.responseRemoveAnnonce(req);
            default -> System.out.println("error");
        }
    }

    private Request receiveRequest() throws IOException, ClassNotFoundException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        int json_len = 0;
        try {
            json_len = this.dis.readInt();
        } catch (IOException ignored) {
            this.server.getLogger().info(new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER, this.id).toString());
            return null;
        }
        byte[] json_bytes = this.dis.readNBytes(json_len);
        String json_str = new String(json_bytes);
        Request request;
        if (!json_str.contains("command")) {
            // encrypted content: extract iv and re-compute json string
            byte[] iv = Arrays.copyOfRange(json_bytes, 0, 16);
            json_str = new String(Arrays.copyOfRange(json_bytes, 16, json_bytes.length));
            String s = decrypt("AES/CBC/PKCS5Padding", json_str, this.sk, new IvParameterSpec(iv));
            Gson gson = new GsonBuilder().registerTypeAdapter(Request.class, new RequestDeserializer()).create();
            request = gson.fromJson(s, Request.class);
        } else {
            Gson gson = new GsonBuilder().registerTypeAdapter(Request.class, new RequestDeserializer()).create();
            request = gson.fromJson(json_str, Request.class);
        }
        return request;
    }

    public void sendRequest(Request request) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        if (request.getCommand() != ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_KO && request.getCommand() != ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK && request.getCommand() != ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV) {
            byte[] iv = this.generateIv();
            String s = encrypt("AES/CBC/PKCS5Padding", new Gson().toJson(request), this.sk, new IvParameterSpec(iv));
            this.dos.writeInt(iv.length + s.getBytes().length);
            this.dos.write(iv);
            this.dos.writeBytes(s);
        } else {
            this.dos.writeInt(new Gson().toJson(request).getBytes().length);
            this.dos.writeBytes(new Gson().toJson(request));
        }
        this.dos.flush();
    }

    @Override
    public void responseToSignUp(Request req) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Request request;
        String mailFromReq = (String) req.getParams().get("Mail");
        String nameFromReq = (String) req.getParams().get("Name");
        if (this.server.isRespondingToRequest())
            if (!this.server.isMailregister(mailFromReq))
                if (this.server.isMailValid(mailFromReq))
                    if (this.server.isNameValid(nameFromReq)) {
                        this.server.addClient(mailFromReq, nameFromReq, (String) req.getParams().get("Pwd"));
                        request = new Request(ProtocolCommand.SIGN_UP_OK, mailFromReq, nameFromReq);
                    } else
                        request = new Request(ProtocolCommand.SIGN_UP_KO, ErrorLogMessage.NAME_NOT_VALID.getContent());
                else
                    request = new Request(ProtocolCommand.SIGN_UP_KO, ErrorLogMessage.MAIL_NOT_VALID.getContent());
            else
                request = new Request(ProtocolCommand.SIGN_UP_KO, ErrorLogMessage.MAIL_ALREADY_TAKEN.getContent());
        else
            request = new Request(ProtocolCommand.SIGN_UP_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent());
        this.sendRequest(request);
    }

    @Override
    public void responseToSignIn(Request req) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Request request;
        String mailFromReq = (String) req.getParams().get("Mail");
        String pwdFromReq = (String) req.getParams().get("Pwd");
        boolean sendDomainList = (boolean) req.getParams().get("SendDomainList");
        if (this.server.isRespondingToRequest())
            if (this.server.isMailregister(mailFromReq))
                if (this.server.isValidPassword(mailFromReq, pwdFromReq)) {
                    this.clientLogged = true;
                    this.mail = mailFromReq;
                    if (sendDomainList)
                        this.responseToDomainList();
                    // the name is returned
                    request = new Request(ProtocolCommand.SIGN_IN_OK, this.server.getClientServerFromMail((String) req.getParams().get("Mail")).getName());
                    //request = new Request(ProtocolCommand.DOMAINS_LIST, (Object) this.server.getDomainList());
                } else
                    request = new Request(ProtocolCommand.SIGN_IN_KO, ErrorLogMessage.COMBINATION_MAIL_PWD_INVALID.getContent());
            else
                request = new Request(ProtocolCommand.SIGN_IN_KO, ErrorLogMessage.COMBINATION_MAIL_PWD_INVALID.getContent());
        else
            request = new Request(ProtocolCommand.SIGN_IN_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent());
        this.sendRequest(request);
    }

    @Override
    public void responseToSignOut(Request req) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request;
        if (this.server.isRespondingToRequest()) {
            request = new Request(ProtocolCommand.SIGN_OUT_OK);
            this.clientLogged = false;
        } else
            request = new Request(ProtocolCommand.SIGN_OUT_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent());
        this.sendRequest(request);
    }

    @Override
    public void responseToCreateAnnonce(Request req) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request;
        if (this.server.isRespondingToRequest()) {
            this.server.addAnnonce(this.mail, (Domain) req.getParams().get("Domain"), (String) req.getParams().get("Title"), (String) req.getParams().get("Descriptif"), (int) req.getParams().get("Price"));
            request = new Request(ProtocolCommand.CREATE_ANNONCE_OK, req.getParams().get("Title"));
        } else
            request = new Request(ProtocolCommand.CREATE_ANNONCE_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent());
        this.sendRequest(request);
    }

    @Override
    public void responseToUpdateAnnonce(Request req) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request;
        if (this.server.isRespondingToRequest()) {
            if (this.server.isOwnerOfAnnonce(this.mail, (int) req.getParams().get("Id"))) {
                // System.out.println(req);
                this.server.updateAnnonce((String) req.getParams().get("Title"), (String) req.getParams().get("Descriptif"), (int) req.getParams().get("Price"), (int) req.getParams().get("Id"));
                request = new Request(ProtocolCommand.UPDATE_ANNONCE_OK);
            } else
                request = new Request(ProtocolCommand.UPDATE_ANNONCE_KO, ErrorLogMessage.NOT_OWNER.getContent());
        } else
            request = new Request(ProtocolCommand.UPDATE_ANNONCE_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent());
        this.sendRequest(request);
    }

    @Override
    public void responseToDomainList() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request;
        if (this.server.isRespondingToRequest()) {
            request = new Request(ProtocolCommand.DOMAINS_LIST_OK, new Object[] {this.server.getDomainList()});
        } else
            request = new Request(ProtocolCommand.DOMAINS_LIST_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent());
        this.sendRequest(request);
    }

    @Override
    public void responseAnnonceFromDomain(Request req) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request;
        Domain domainFromReq = (Domain) req.getParams().get("Domain");
        if (this.server.isRespondingToRequest()) {
            Annonce[] annonces = this.server.getAnnonceListOfDomain(domainFromReq);
            if (annonces.length != 0)
                request = new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN_OK, (Object) annonces);
            else
                request = new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN_KO, ErrorLogMessage.NO_ANNONCE_IN_THAT_DOMAIN.getContent());
        } else {
            request = new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent());
        }
        this.sendRequest(request);
    }

    @Override
    public void responseRemoveAnnonce(Request req) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request;
        int idFromReq = (int) req.getParams().get("Id");
        if (this.server.isRespondingToRequest()) {
            if (this.server.isOwnerOfAnnonce(this.mail,  idFromReq)) {
                this.server.removeAnnonce(idFromReq);
                request = new Request(ProtocolCommand.REMOVE_ANNONCE_OK);
            } else
                request = new Request(ProtocolCommand.REMOVE_ANNONCE_KO, ErrorLogMessage.NOT_OWNER.getContent());
        } else
            request = new Request(ProtocolCommand.REMOVE_ANNONCE_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent());
        this.sendRequest(request);
    }

    @Override
    public void responseToRequestPublicKey(Request req) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        Request request;
        if (this.server.isRespondingToRequest()) {
            this.performKeyAgreement(req);
            this.iv = (byte[]) req.getParams().get("IV");
            request = new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()});
        } else
            request = new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent());
        this.sendRequest(request);
    }

    public SecretKey getKeyFromSecret(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public byte[] generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
        //return new IvParameterSpec(iv);
    }

    public String encrypt(String algorithm, String input, SecretKey key, IvParameterSpec iv) throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder().encodeToString(cipherText);
    }

    public String decrypt(String algorithm, String cipherText, SecretKey key, IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(plainText);
    }
}
