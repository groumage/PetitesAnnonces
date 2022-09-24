package test;

import main.client.Client;
import main.protocol.*;
import main.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.swing.*;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.LogManager;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.fieldIn;
import static org.hamcrest.Matchers.notNullValue;

public class Gui {

    private Server server;
    private Client alice = null;
    private Client bob = null;
    private final String mailAlice = "alice@gmail.com";
    private final String pwdAlice = "test";
    private final String usernameAlice = "Alice";
    private final String mailBob = "bob@gmail.com";
    private final String pwdBob = "test2";
    private final String usernameBob = "Bob";
    private final String aliceWrongMail = "alice@toto.com";
    private final String aliceWrongUsername = "Prout";
    private final String annonce1Title = "Beautiful house";
    private final String annonce1TitleUpdated = "Very beautiful house";
    private final String annonce1Content = "Very big house";
    private final String annonce1ContentUpdated = "Very much big house";
    private final int annonce1Price = 1000;
    private final int annonce1PriceUpdated = 10000;
    private final String annonce2Title = "Beautiful house 2";
    private final String annonce2Content = "Very big house 2";
    private final int annonce2Price = 2000;

    @Before
    public void initServerAndClient() throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InterruptedException, InvalidKeySpecException {
        this.server = new Server(true, false);
        this.alice = new Client(mailAlice, usernameAlice, false);
        new Thread(this.server).start();
    }

    @Test
    public void signOutOk() {
        this.signInOk();
        this.signOut(this.alice);
    }

    @Test
    public void signOutKo() {
        this.signInOk();
        this.server.setRespondingToRequest(false);
        this.signOut(this.alice, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST);
    }

    private void removeAnnonce(Client client, Domain dom, String title) {
        this.alice.getGui().clickOnDomain(dom);
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_OK);
        client.getGui().clickOnAnnonce(title);
        client.getGui().clickRemoveAnnonce();
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.REMOVE_ANNONCE_OK);
        assert this.getLastMessageConsole(this.alice).equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_REMOVE_ANNONCE_OK).toString().substring(11));
    }

    private void removeAnnonce(Client client, Domain dom, String title, ErrorLogMessage error) {
        this.alice.getGui().clickOnDomain(dom);
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_OK);
        client.getGui().clickOnAnnonce(title);
        client.getGui().clickRemoveAnnonce();
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.REMOVE_ANNONCE_KO);
        assert this.getLastMessageConsole(this.alice).equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_REMOVE_ANNONCE_KO, error.getContent()).toString().substring(11));
    }

    @Test
    public void removeAnnonceOk() {
        this.createAnnonceOk();
        this.alice.getGui().clickOnDomain(Domain.HOUSE);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_OK);
        this.alice.getGui().clickOnAnnonce(annonce1Title);
        assert this.alice.getGui().getRemoveAnnonceButton().isEnabled();
        this.removeAnnonce(this.alice, Domain.HOUSE, annonce1Title);
    }

    @Test
    public void removeAnnonceKo() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, BadPaddingException, InvalidKeyException, InterruptedException {
        this.createAnnonceOk();
        this.bob = new Client(mailBob, usernameBob, false);
        this.createAnnonceOkCustom(this.bob, mailBob, pwdAlice, usernameBob, annonce2Title, Domain.HOUSE, annonce2Content, annonce2Price);
        this.alice.getGui().clickOnDomain(Domain.HOUSE);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_OK);
        this.alice.getGui().clickOnAnnonce(annonce2Title);
        assert !this.alice.getGui().getRemoveAnnonceButton().isEnabled();
        this.server.setRespondingToRequest(false);
        this.removeAnnonce(this.alice, Domain.HOUSE, annonce1Title, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST);
    }

    private void updateAnnonce(Client client, Domain dom, String title, String titleUpdated, String contentUpdated, int priceUpdated) {
        assert client.getGui().getDomainJList().isEnabled();
        assert client.getGui().getAnnonceJList().isEnabled();
        client.getGui().clickOnDomain(dom);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_OK);
        client.getGui().clickOnAnnonce(title);
        client.getGui().clickUpdateCheckBox();
        assert !client.getGui().getDomainJList().isEnabled();
        assert !client.getGui().getAnnonceJList().isEnabled();
        client.getGui().getTitleAnnonceField().setText(titleUpdated);
        client.getGui().getContentAnnonce().setText(contentUpdated);
        client.getGui().getPriceField().setText(String.valueOf(priceUpdated));
        client.getGui().clickUpdateAnnonce();
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.UPDATE_ANNONCE_OK);
        assert this.getLastMessageConsole(client).equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_UPDATED_OK).toString().substring(11));
        client.getGui().clickUpdateCheckBox();
        assert client.getGui().getDomainJList().isEnabled();
        assert client.getGui().getAnnonceJList().isEnabled();
    }

    private void updateAnnonce(Client client, Domain dom, String title, String titleUpdated, String contentUpdated, int priceUpdated, ErrorLogMessage error) throws InterruptedException {
        assert client.getGui().getDomainJList().isEnabled();
        assert client.getGui().getAnnonceJList().isEnabled();
        client.getGui().clickOnDomain(dom);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_OK);
        client.getGui().clickOnAnnonce(title);
        client.getGui().clickUpdateCheckBox();
        assert !client.getGui().getDomainJList().isEnabled();
        assert !client.getGui().getAnnonceJList().isEnabled();
        client.getGui().getTitleAnnonceField().setText(titleUpdated);
        client.getGui().getContentAnnonce().setText(contentUpdated);
        client.getGui().getPriceField().setText(String.valueOf(priceUpdated));
        client.getGui().clickUpdateAnnonce();
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.UPDATE_ANNONCE_KO);
        assert this.getLastMessageConsole(client).equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_UPDATED_KO, error.getContent()).toString().substring(11));
        client.getGui().clickUpdateCheckBox();
        assert client.getGui().getDomainJList().isEnabled();
        assert client.getGui().getAnnonceJList().isEnabled();
    }

    @Test
    public void updateAnnonceOk() throws InterruptedException {
        this.createAnnonceOk();
        this.updateAnnonce(this.alice, Domain.HOUSE, annonce1Title, annonce1TitleUpdated, annonce1ContentUpdated, annonce1PriceUpdated);
    }

    @Test
    public void updateAnnonceKo() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, BadPaddingException, InvalidKeyException, InterruptedException {
        this.createAnnonceOk();
        this.bob = new Client(mailBob, usernameBob, false);
        this.createAnnonceOkCustom(this.bob, mailBob, pwdAlice, usernameBob, annonce2Title, Domain.HOUSE, annonce2Content, annonce2Price);
        this.alice.getGui().clickOnDomain(Domain.HOUSE);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_OK);
        this.alice.getGui().clickOnAnnonce(annonce2Title);
        assert !this.alice.getGui().getUpdateCheckBox().isEnabled();
        this.server.setRespondingToRequest(false);
        this.alice.getGui().clickOnAnnonce(annonce1Title);
        this.updateAnnonce(this.alice, Domain.HOUSE, annonce1Title, annonce1TitleUpdated, annonce1ContentUpdated, annonce1PriceUpdated, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST);
    }

    private void createAnnonceOkCustom(Client client, String mail, String pwd, String name, String title, Domain dom, String content, int price) {
        this.connect(client);
        this.signUp(client, mail, pwd, name);
        this.signIn(client, mail, pwd);
        this.createAnnonce(client, title, dom, content, price);
    }

    @Test
    public void createAnnonceOk() {
        this.createAnnonceOkCustom(this.alice, mailAlice, pwdAlice, usernameAlice, annonce1Title, Domain.HOUSE, annonce1Content, annonce1Price);
    }

    @Test
    public void createAnnonceKo() {
        this.signInOk();
        this.server.setRespondingToRequest(false);
        this.createAnnonce(this.alice, annonce1Title, Domain.HOUSE, annonce1Content, annonce1Price, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST);
    }

    private void createAnnonceProcess(Client client, String title, Domain dom, String content, int price) {
        assert client.getGui().getDomainJList().isEnabled();
        assert client.getGui().getAnnonceJList().isEnabled();
        client.getGui().clickCreateCheckBox();
        assert this.domainInComboBox(client, dom);
        assert !client.getGui().getDomainJList().isEnabled();
        assert !client.getGui().getAnnonceJList().isEnabled();
        client.getGui().getTitleAnnonceField().setText(title);
        client.getGui().getDomainComboBox().setSelectedItem(dom);
        client.getGui().getContentAnnonce().setText(content);
        client.getGui().getPriceField().setText(String.valueOf(price));
        client.getGui().clickCreateAnnonce();
        client.getGui().clickCreateCheckBox();
        assert client.getGui().getDomainJList().isEnabled();
        assert client.getGui().getAnnonceJList().isEnabled();
    }

    private void createAnnonce(Client client, String title, Domain dom, String content, int price) {
        this.createAnnonceProcess(client, title, dom, content, price);
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.CREATE_ANNONCE_OK);
        assert this.getLastMessageConsole(client).equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_CREATED_OK, title).toString().substring(11));
    }

    private void createAnnonce(Client client, String title, Domain dom, String content, int price, ErrorLogMessage error) {
        this.createAnnonceProcess(client, title, dom, content, price);
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.CREATE_ANNONCE_KO);
        assert this.getLastMessageConsole(client).equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_CREATED_KO, error.getContent()).toString().substring(11));
    }

    private boolean domainInComboBox(Client client, Domain dom) {
        for (int i = 0; i < client.getGui().getDomainComboBox().getModel().getSize(); i++)
            if (client.getGui().getDomainComboBox().getModel().getElementAt(i) == dom)
                return true;
        return false;
    }
    
    private void connect(Client client) {
        client.getGui().clickConnect();
        await().until(fieldIn(client).ofType(SecretKey.class), notNullValue());
        assert this.getLastMessageConsole(client).equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString().substring(11));
    }
    
    //todo: sign up with empty name
    private void signUpProcess(Client client, String mail, String pwd, String name) {
        client.getGui().getMailField().setText(mail);
        client.getGui().getPwdField().setText(pwd);
        client.getGui().getUsernameField().setText(name);
        client.getGui().clickSignUp();
    }

    private void signUp(Client client, String mail, String pwd, String name) {
        this.signUpProcess(client, mail, pwd, name);
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        assert this.getLastMessageConsole(client).equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, mail, name).toString().substring(11));
    }
    
    private void signUp(Client client, String mail, String pwd, String name, ErrorLogMessage error) {
        this.signUpProcess(client, mail, pwd, name);
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_KO);
        assert this.getLastMessageConsole(client).equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_KO, error.getContent()).toString().substring(11));
    }
    
    private void signInProcess(Client client, String mail, String pwd) {
        client.getGui().setMail(mail);
        client.getGui().setPwd(pwd);
        client.getGui().clickSignIn();
    }
    
    private void signIn(Client client, String mail, String pwd) {
        this.signInProcess(client, mail, pwd);
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        assert this.getLastMessageConsole(client).equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, mail).toString().substring(11));
    }

    private void signIn(Client client, String mail, String pwd, ErrorLogMessage error) {
        this.signInProcess(client, mail, pwd);
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_KO);
        assert this.aliceGetLastMessageConsole().equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_KO, error.getContent()).toString().substring(11));
    }

    private void signOut(Client client) {
        client.getGui().clickSignOut();
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.SIGN_OUT_OK);
        assert this.aliceGetLastMessageConsole().equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_OUT_OK).toString().substring(11));
    }

    private void signOut(Client client, ErrorLogMessage error) {
        client.getGui().clickSignOut();
        await().until(() -> client.getLastRequest().getCommand() == ProtocolCommand.SIGN_OUT_KO);
        assert this.aliceGetLastMessageConsole().equals(new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_OUT_KO, error.getContent()).toString().substring(11));
    }

    @Test
    public void signUpOk() {
        this.connect(this.alice);
        this.signUp(this.alice, mailAlice, pwdAlice, usernameAlice);
    }

    @Test
    public void signUpKo() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, BadPaddingException, InvalidKeyException, InterruptedException {
        this.bob = new Client(mailBob, usernameBob, false);
        this.connect(this.alice);
        this.signUp(this.alice, aliceWrongMail, pwdAlice, usernameAlice, ErrorLogMessage.MAIL_NOT_VALID);
        this.signUp(this.alice, mailAlice, pwdAlice, aliceWrongUsername, ErrorLogMessage.NAME_NOT_VALID);
        this.connect(this.bob);
        this.signUp(this.bob, mailAlice, pwdBob, usernameBob);
        this.signUp(this.alice, mailAlice, pwdAlice, usernameAlice, ErrorLogMessage.MAIL_ALREADY_TAKEN);
        this.server.setRespondingToRequest(false);
        this.signUp(this.alice, mailAlice, pwdAlice, usernameAlice, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST);
    }

    @Test
    public void signInOk() {
        this.signUpOk();
        this.signIn(this.alice, mailAlice, pwdAlice);
    }

    @Test
    public void signInKo() {
        this.signUpOk();
        this.signIn(this.alice, aliceWrongMail, pwdAlice, ErrorLogMessage.COMBINATION_MAIL_PWD_INVALID);
        this.signIn(this.alice, mailAlice, "test000", ErrorLogMessage.COMBINATION_MAIL_PWD_INVALID);
        this.server.setRespondingToRequest(false);
        this.signIn(this.alice, mailAlice, pwdAlice, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST);
    }

    private String aliceGetLastMessageConsole() {
        if (!this.alice.getMessage("Console").contains("\n"))
            return this.alice.getMessage("Console");
        else
            return this.alice.getMessage("Console").substring(this.alice.getMessage("Console").lastIndexOf("\n")).replace("\n", "");
    }

    private String getLastMessageConsole(Client client) {
        if (!client.getMessage("Console").contains("\n"))
            return client.getMessage("Console");
        else
            return client.getMessage("Console").substring(client.getMessage("Console").lastIndexOf("\n")).replace("\n", "");
    }

    @After
    public void stopServerAndStopClient() throws IOException {
        this.alice.stopProcess();
        this.server.stopProcess();
        if (this.bob != null) {
            this.bob.stopProcess();
            this.bob = null;
        }
        LogManager logManager = LogManager.getLogManager();
        logManager.reset();
    }
}
