package main.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import main.gui.PerspectiveView;
import main.protocol.*;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import main.server.Annonce;
import main.gui.gui;
import org.apache.commons.codec.binary.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Client implements Runnable, ClientToServerRequestProtocol, ClientProcessResponseProtocol {
    private static final boolean TEST = true;
    private String mail;
    private String name;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private KeyPair kp;
    private Thread receiveCmd;
    //private byte[] sharedSecretServer;
    private ClientState state;
    private byte[] iv;
    private SecretKey sk = null;
    private boolean stopReceiveCommand = false;
    private boolean stopProcess = false;
    private Logger logger;
    private FileHandler fh;
    private Request lastRequest;
    private JFrame frame;
    private gui gui;
    private Annonce[] annonces;
    private Domain[] domains;
    private HashMap<String, String> messages;

    public Client(String mail, String name, boolean setVisibleFrame) throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InterruptedException {
        this.logger = Logger.getLogger("LogClient_" + mail);
        fh = new FileHandler("LogClient_" + mail + ".log");
        this.logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        this.logger.setUseParentHandlers(false); // do not print on console

        this.state = ClientState.DISCONNECTED;
        this.logger.info("[STATUS] " + this.state);

        this.initializeKey();
        this.messages = new HashMap<>();
        this.messages.put("Console", "");

        this.frame = new JFrame("App");
        this.gui = new gui(this);
        this.frame.setContentPane(this.gui.getPane());
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.pack();
        this.frame.setVisible(setVisibleFrame);
    }

    public void openConnection() throws IOException {
        try {
            this.socket = new Socket("127.0.0.1", 1234);
        } catch (IOException e) {
            this.logger.info(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_UNREACHABLE_SERVER).toString());
        }
        if (this.socket != null) {
            this.dis = new DataInputStream(this.socket.getInputStream());
            this.dos = new DataOutputStream(this.socket.getOutputStream());
            String logMsg = new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString();
            this.logger.info(logMsg);
            this.addConsoleMessage(logMsg);
            this.setState(ClientState.CONNECTED);
            this.getGui().changeView(PerspectiveView.CONNECTED);
            this.logger.info("[STATUS] " + this.state);
            this.receiveCmd = new Thread(() -> {
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
            });
            this.stopReceiveCommand = false;
            this.receiveCmd.start();
        }
    }

    public Request receiveRequest() throws IOException, ClassNotFoundException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        int json_len;
        try {
            json_len = this.dis.readInt();
        } catch (IOException ignored) {
            return null;
        }
        byte[] json_bytes = this.dis.readNBytes(json_len);
        String json_str = new String(json_bytes);
        Request request;
        if (!json_str.contains("command")) {
            // there is no "command" string line so the content is encrypted content
            // thus we extract iv (firsy 16 bytes) and re-compute json string in order to have request content
            byte[] iv = Arrays.copyOfRange(json_bytes, 0, 12);
            json_str = new String(Arrays.copyOfRange(json_bytes, 12, json_bytes.length));
            String s = decrypt("AES/GCM/NoPadding", json_str, this.sk, iv);
            Gson gson = new GsonBuilder().registerTypeAdapter(Request.class, new RequestDeserializer()).create();
            request = gson.fromJson(s, Request.class);
        } else {
            Gson gson = new GsonBuilder().registerTypeAdapter(Request.class, new RequestDeserializer()).create();
            request = gson.fromJson(json_str, Request.class);
        }
        this.logger.info("[RECEIVE] " + request);
        return request;
    }

    public void processInput(Request req) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        switch (req.getCommand()) {
            case REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK -> this.requestPublicKeyOk(req);
            case REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_KO -> this.requestPublicKeyKo(req);
            case SIGN_UP_OK -> this.signUpOk(req);
            case SIGN_UP_KO -> this.signUpKo(req);
            case SIGN_IN_OK -> this.signInOk(req);
            case SIGN_IN_KO -> this.signInKo(req);
            case SIGN_OUT_OK -> this.signOutOk();
            case SIGN_OUT_KO -> this.signOutKo();
            case CREATE_ANNONCE_OK -> this.createAnnonceOk(req);
            case CREATE_ANNONCE_KO -> this.createAnnonceKo(req);
            case UPDATE_ANNONCE_OK -> this.updateAnonceOk(req);
            case UPDATE_ANNONCE_KO -> this.updateAnonceKo(req);
            case DOMAINS_LIST_OK -> this.domainListOk(req);
            case DOMAINS_LIST_KO -> this.domainListKo(req);
            case ANNONCE_FROM_DOMAIN_OK -> this.annonceFromDomainOk(req);
            case ANNONCE_FROM_DOMAIN_KO -> this.annonceFromDomainKo(req);
            case REMOVE_ANNONCE_OK -> this.removeAnnonceOk(req);
            case REMOVE_ANNONCE_KO -> this.removeAnnonceKo(req);
            default -> System.out.println("error 2");
        }
        // at this point, the last request has been already processed
        this.lastRequest = req;
    }

    public void closeConnection() throws IOException, NoSuchAlgorithmException {
        if (this.socket != null) {
            this.sk = null;
            this.initializeKey(); // reinitialize to have different secret key at the next connection
            this.receiveCmd.interrupt();
            this.dis.close();
            this.dos.close();
            this.socket.close();
            String logMsg = new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_CLOSED).toString();
            this.logger.info(logMsg);
            this.addConsoleMessage(logMsg);
            this.state = ClientState.DISCONNECTED;
            this.logger.info("[STATUS] " + this.state);
        }
    }

    public void stopProcess() {
        this.stopProcess = true;
    }

    private void initializeKey() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        this.kp = kpg.generateKeyPair();
    }

    public void sendRequest(Request request) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.logger.info("[SEND] " + request);
        if ((request.getCommand() != ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK) && (request.getCommand() != ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV)) {
            // todo: add throw error if sk is null
            byte[] iv = this.generate16Bytes();
            String s = encrypt("AES/GCM/NoPadding", new Gson().toJson(request), this.sk, iv);
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
    public void requestPublicKeyAndIV() throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.iv = this.generate16Bytes();
        Request request = new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, (Object) this.kp.getPublic().getEncoded(), this.iv);
        this.sendRequest(request);
    }

    @Override
    public void signUp(String mail, String name, String pwd) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request = new Request(ProtocolCommand.SIGN_UP, mail, name, pwd);
        this.sendRequest(request);
    }

    @Override
    public void signIn(String mail, String pwd, boolean sendDomainList) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request = new Request(ProtocolCommand.SIGN_IN, mail, pwd, sendDomainList);
        this.mail = mail;
        this.sendRequest(request);
    }

    @Override
    public void signOut() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request = new Request(ProtocolCommand.SIGN_OUT);
        this.sendRequest(request);
    }

    @Override
    public void createAnnonce(Domain dom, String title, String descriptif, int price) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request = new Request(ProtocolCommand.CREATE_ANNONCE, dom, title, descriptif, price);
        this.sendRequest(request);
    }

    @Override
    public void updateAnnonce(String title, String descriptif, int price, int id) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request = new Request(ProtocolCommand.UPDATE_ANNONCE, title, descriptif, price, id);
        this.sendRequest(request);
    }

    @Override
    public void domainList() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request = new Request(ProtocolCommand.DOMAINS_LIST);
        this.sendRequest(request);
    }

    @Override
    public void annonceFromDomain(Domain dom) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request = new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN, dom);
        this.sendRequest(request);
    }

    @Override
    public void removeAnnonce(int id) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Request request = new Request(ProtocolCommand.REMOVE_ANNONCE, id);
        this.sendRequest(request);
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

    @Override
    public void run() {
        while (!this.stopProcess) {

        }
    }

    public static void main (String[] args) throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InterruptedException {
        new Thread(new Client("alice@gmail.com", "Alice", true)).start();
    }

    @Override
    public void requestPublicKeyOk(Request req) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException {
        this.performKeyAgreement(req);
    }

    private void performKeyAgreement(Request req) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException {
        byte[] otherPk = (byte[]) req.getParams().get("PublicKey");
        KeyFactory kf = KeyFactory.getInstance("EC");
        X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(otherPk);
        PublicKey otherPublicKey = kf.generatePublic(pkSpec);

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(this.kp.getPrivate());
        ka.doPhase(otherPublicKey, true);
        byte[] sharedSecret = ka.generateSecret();

        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        hash.update(sharedSecret);
        List<ByteBuffer> keys = Arrays.asList(ByteBuffer.wrap(this.kp.getPublic().getEncoded()), ByteBuffer.wrap(otherPk));
        Collections.sort(keys);
        hash.update(keys.get(0));
        hash.update(keys.get(1));

        byte[] derivedKey = hash.digest();
        this.sk = this.getKeyFromSecret(encodeHexString(derivedKey), "key");
        //this.logger.info("[CLIENT] The shared secret is: " + encodeHexString(derivedKey));
        //System.out.println("[CLIENT] The shared secret is: " + encodeHexString(derivedKey));
    }

    @Override
    public void requestPublicKeyKo(Request req) {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_REQUEST_PUBLIC_KEY_KO, req.getParams().get("Error")).toString());
    }

    @Override
    public void signUpOk(Request req) {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, req.getParams().get("Mail"), req.getParams().get("Name")).toString());
    }

    @Override
    public void signUpKo(Request req) {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_KO, req.getParams().get("Error")).toString());
    }

    @Override
    public void signInOk(Request req) {
        //this.state = ClientState.LOGGED;
        this.gui.changeView(PerspectiveView.LOGGED);
        this.setState(ClientState.LOGGED);
        this.logger.info("[STATUS] " + this.state);
        this.name = (String) req.getParams().get("Name");
        String logMsg = new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, this.mail).toString();
        this.logger.info(logMsg);
        this.addConsoleMessage(logMsg);
    }

    @Override
    public void signInKo(Request req) {
        this.mail = "";
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_KO, req.getParams().get("Error")).toString());
    }

    @Override
    public void signOutOk() {
        this.mail = "";
        this.name = "";
        this.setState(ClientState.CONNECTED);
        this.logger.info("[STATUS] " + this.state);
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_OUT_OK).toString());
    }

    @Override
    public void signOutKo() {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_OUT_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()).toString());
    }

    @Override
    public void createAnnonceOk(Request req) {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_CREATED_OK, req.getParams().get("Title")).toString());
    }

    @Override
    public void createAnnonceKo(Request req) {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_CREATED_KO, req.getParams().get("Error")).toString());
    }

    @Override
    public void updateAnonceOk(Request req) {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_UPDATED_OK).toString());
    }

    @Override
    public void updateAnonceKo(Request req) {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_UPDATED_KO, req.getParams().get("Error")).toString());
    }

    @Override
    public void domainListOk(Request req) {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_DOMAIN_LIST_OK, Arrays.toString((Domain[]) req.getParams().get("Domains"))).toString());
        //this.gui.updateDomainList((Domain[]) req.getParams().get("Domains"));
        this.gui.setDomainList((Domain[]) req.getParams().get("Domains"));
    }

    @Override
    public void domainListKo(Request req) {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_DOMAIN_LIST_KO, req.getParams().get("Error")).toString());
    }

    @Override
    public void annonceFromDomainOk(Request req) {
        //System.out.println(((Annonce[]) req.getParams().get("AnnoncesFromDomain"))[1]);
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_FROM_DOMAIN_OK, Arrays.toString((Annonce[]) req.getParams().get("AnnoncesFromDomain"))).toString());
        this.annonces = (Annonce[]) req.getParams().get("AnnoncesFromDomain");
        this.gui.updateAnnonceList();
    }

    @Override
    public void annonceFromDomainKo(Request req) {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_FROM_DOMAIN_KO, req.getParams().get("Error")).toString());
        this.annonces = new Annonce[]{};
        this.gui.updateAnnonceList();
    }

    @Override
    public void removeAnnonceOk(Request req) {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_REMOVE_ANNONCE_OK).toString());
    }

    @Override
    public void removeAnnonceKo(Request req) {
        this.addMessageToLoggerAndConsole(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_REMOVE_ANNONCE_KO, req.getParams().get("Error")).toString());
    }

    public SecretKey getKeyFromSecret(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public byte[] generate16Bytes() {
        if (TEST)
            return "initializaVe".getBytes();
        else {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            return iv;
        }
    }

    public String encrypt(String algorithm, String input, SecretKey key, byte[] iv) throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        cipher.updateAAD("v1".getBytes(UTF_8));
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return new String(Base64.encodeBase64(cipherText));
    }

    public String decrypt(String algorithm, String cipherText, SecretKey key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        cipher.updateAAD("v1".getBytes(UTF_8));
        byte[] plainText = cipher.doFinal(java.util.Base64.getDecoder().decode(cipherText));
        return new String(plainText);
    }

    public Request getLastRequest() {
        return this.lastRequest;
    }

    public String getMail() {
        return this.mail;
    }

    public String getName() {
        return this.name;
    }

    public void setState(ClientState state) {
        this.state = state;
        this.gui.setStatusLabel(state);
    }

    public gui getGui() {
        return this.gui;
    }

    public Annonce[] getAnnonces() {
        return this.annonces;
    }

    public void setDomainsList(Domain[] domains) {
        this.domains = domains;
    }

    public Domain[] getDomains() {
        return this.domains;
    }

    public void addConsoleMessage(String msg) {
        msg = msg.substring(11);
        if (this.messages.get("Console").equals(""))
            this.messages.put("Console", msg); // do not print "[INTERNAL]"
        else
            this.messages.put("Console", this.messages.get("Console") + "\n" + msg); // do not print "[INTERNAL]"
        this.gui.highlightDst("Console");
    }

    public String getMessage(String dst) {
        return this.messages.get(dst);
    }

    private void addMessageToLoggerAndConsole(String msg) {
        this.logger.info(msg);
        this.addConsoleMessage(msg);
    }

    public PublicKey getPublicKey() {
        return this.kp.getPublic();
    }

    public HashMap<String, String> getMessages() {
        return this.messages;
    }

}
