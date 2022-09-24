package main.server;


import main.protocol.Domain;
import main.protocol.TokenInternalLogMessage;
import main.protocol.InternalLogMessage;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server implements Runnable {
    private int clientHandlerId = 0;
    private int annonceId = 0;
    private KeyPair kp;
    private ServerSocket server;
    private ArrayList<ClientHandler> clientsHandler;
    private ArrayList<ClientServer> clientsServer;
    private ArrayList<Annonce> annonces;
    private ArrayList<Domain> domains;
    private boolean isRespondingToRequest = true;
    private boolean stopProcess = false;
    private Logger logger;
    private FileHandler fh;

    public boolean isMailregister(String mail) {
        for (ClientServer cs : this.clientsServer)
            if (cs.getMail().equals(mail))
                return true;
        return false;
    }

    public ClientServer getClientServerFromMail(String mail) {
        for (ClientServer cs : this.clientsServer)
            if (cs.getMail().equals(mail))
                return cs;
        return null;
    }

    private String getEncryptedPassword(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String algorithm = "PBKDF2WithHmacSHA1";
        int derivedKeyLength = 160;
        int iterations = 20000;

        byte[] saltBytes = Base64.getDecoder().decode(salt);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, iterations, derivedKeyLength);
        SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);

        byte[] encBytes = f.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(encBytes);
    }

    private String getNewSalt() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[8];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public Server(boolean isRespondingToRequest, boolean testServer) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        this.initializeKey();
        this.server = new ServerSocket(1234);
        this.server.setReuseAddress(true);
        this.clientsHandler = new ArrayList<>();
        this.clientsServer = new ArrayList<>();
        this.annonces = new ArrayList<>();
        this.domains = new ArrayList<>();
        this.domains.add(Domain.HOUSE);
        this.domains.add(Domain.CAR);
        this.isRespondingToRequest = isRespondingToRequest;

        this.logger = Logger.getLogger("LogServer");
        fh = new FileHandler("LogServer.log");
        this.logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        this.logger.setUseParentHandlers(false); // do not print on console

        if (testServer) {
            this.addClient("alice@gmail.com", "Alice", "test");
            this.addClient("bob@gmail.com", "Bob", "test");
            this.annonces.add(new Annonce("alice@gmail.com", Domain.HOUSE, "Big House", "Content of the annonce", 1000, 0));
            this.annonces.add(new Annonce("bob@gmail.com", Domain.HOUSE, "Big House 2", "Content of the annonce 2", 2000, 1));
        }
    }

    public void setRespondingToRequest(boolean respondingToRequest) {
        this.isRespondingToRequest = respondingToRequest;
    }

    private void initializeKey() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        this.kp = kpg.generateKeyPair();
    }

    public static void main(String[] arg) throws NoSuchAlgorithmException, IOException, ClassNotFoundException, InvalidKeySpecException {
        new Thread(new Server(true, true)).start();
    }

    @Override
    public void run() {
        while (!this.stopProcess) {
            ClientHandler ch = null;
            try {
                ch = new ClientHandler(this.server.accept(), this, this.clientHandlerId);
                this.clientHandlerId++;
            } catch (IOException e) {
                //this.stopProcess = true;
            }
            new Thread(ch).start();
            this.clientsHandler.add(ch);
        }
    }

    public PublicKey getPk() {
        return this.kp.getPublic();
    }

    public PrivateKey getPrivate() {
        return this.kp.getPrivate();
    }

    public boolean isRespondingToRequest() {
        return this.isRespondingToRequest;
    }

    public boolean isNameValid(String name) {
        String[] forbiddenName = {"Prout"};
        for (String s: forbiddenName)
            if (s.equals(name))
                return false;
        return true;
    }

    public boolean isMailValid(String mail) {
        // only gmail address accepted
        String expression = "^[\\w.+\\-]+@gmail\\.com$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(mail);
        return matcher.matches();
    }

    public synchronized void addClient(String mail, String name, String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String salt = this.getNewSalt();
        String encryptedPassword = this.getEncryptedPassword(password, salt);
        this.clientsServer.add(new ClientServer(mail, name, encryptedPassword, salt));
        this.logger.info(new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, mail, name).toString());
    }

    public boolean isValidPassword(String mail, String pwd) throws NoSuchAlgorithmException, InvalidKeySpecException {
        ClientServer cs = this.getClientServerFromMail(mail);
        String salt = cs.getSalt();
        String computedEncryptedPwd = this.getEncryptedPassword(pwd, salt);
        return computedEncryptedPwd.equals(cs.getEncryptedPwd());
    }

    public void stopProcess() throws IOException {
        this.stopProcess = true;
        this.server.close();
    }

    public void closeConnection() throws IOException {
        this.server.close();
    }

    public synchronized Logger getLogger() {
        return logger;
    }
    // todo: unit test of server function
    public synchronized void addAnnonce(String mail, Domain dom, String title, String descriptif, int price) {
        Annonce newAnnonce = new Annonce(mail, dom, title, descriptif, price, this.annonceId);
        this.annonces.add(newAnnonce);
        this.logger.info(new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CREATE_ANNONCE, mail, title, descriptif, dom, price, this.annonceId).toString());
        this.annonceId++;
    }

    public boolean isOwnerOfAnnonce(String mail, int id) {
        for (Annonce a: this.annonces) {
            if (a.getOwner().equals(mail) && a.getId() == id)
                return true;
        }
        return false;
    }

    public void updateAnnonce(String title, String descriptif, int price, int id) {
        for (Annonce a: this.annonces) {
            if (a.getId() == id) {
                a.setTitle(title);
                a.setDescriptif(descriptif);
                a.setPrice(price);
                this.logger.info(new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_UPDATE_ANNONCE, a.getOwner(), a.getTitle(), a.getContent(), a.getDomain(), a.getPrice(), a.getId()).toString());
                break;
            }
        }
    }

    private String getNameFromMail(String mail) {
        for (ClientServer cs: this.clientsServer) {
            if (cs.getMail().equals(mail))
                return cs.getName();
        }
        return null;
    }

    public Domain[] getDomainList() {
        return this.domains.toArray(new Domain[0]);
    }

    public Annonce[] getAnnonceListOfDomain(Domain d) {
        ArrayList<Annonce> annoncesFiltered = new ArrayList<>();
        for (Annonce a: this.annonces) {
            // replace mail with username
            annoncesFiltered.add(new Annonce(this.getNameFromMail(a.getOwner()), a.getDomain(), a.getTitle(), a.getContent(), a.getPrice(), a.getId()));
        }
        annoncesFiltered.removeIf(a -> a.getDomain() != d);
        return annoncesFiltered.toArray(new Annonce[0]);
    }

    public void removeAnnonce(int idFromReq) {
        for (Annonce a: this.annonces)
            if (a.getId() == idFromReq)
                this.logger.info(new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_REMOVE_ANNONCE, a.getOwner(), a.getTitle(), a.getContent(), a.getDomain(), a.getPrice(), a.getId()).toString());
        this.annonces.removeIf(a -> a.getId() == idFromReq);
    }
}
