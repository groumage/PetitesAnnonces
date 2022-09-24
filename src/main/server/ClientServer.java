package main.server;

public class ClientServer {
    private String mail;
    private String name;
    private String encryptedPwd;
    private String salt;

    public ClientServer(String mail, String name, String encryptedPwd, String salt) {
        this.mail = mail;
        this.name = name;
        this.encryptedPwd = encryptedPwd;
        this.salt = salt;
    }

    public String getName() {
        return this.name;
    }

    public String getMail() {
        return this.mail;
    }

    public String getSalt() {
        return this.salt;
    }

    public String getEncryptedPwd() {
        return this.encryptedPwd;
    }
}
