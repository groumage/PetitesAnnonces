package test;

import main.client.Client;
import main.client.ClientState;
import main.protocol.*;
import main.server.Annonce;
import main.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.logging.LogManager;

import static org.awaitility.Awaitility.*;
import static org.hamcrest.Matchers.notNullValue;

public class Protocol {

    Server server;
    Client alice;
    Client bob = null;
    String pwdHash;

    @Before
    public void initServerAndClient() throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InterruptedException, InvalidKeySpecException {
        this.server = new Server(true, false);
        this.alice = new Client("alice@gmail.com", "Alice", false);
        new Thread(this.server).start();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] pwdHash = digest.digest(new BufferedReader(new FileReader("pwd")).readLine().getBytes(StandardCharsets.UTF_8));
        this.pwdHash = Arrays.toString(pwdHash);
    }

    @Test
    public void signUpOk() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);

    }

    /**
     * Tested errors: mail not valid, mal already taken, not responding to request
     * @throws IOException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws NoSuchAlgorithmException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws InterruptedException
     */
    @Test
    public void signUpKo() throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InterruptedException {
        this.bob = new Client("bob@gmail.com", "Bob", false); // bob will take the mail alice want so alice get already taken mail error

        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@toto.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_KO);
        this.alice.signUp("alice@gmail.com", "Prout", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_KO);
        this.alice.signUp("alice@toto.com", "Prout", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_KO);

        this.bob.openConnection();
        this.bob.requestPublicKeyAndIV();
        await().until(fieldIn(this.bob).ofType(SecretKey.class), notNullValue());
        this.bob.signUp("alice@gmail.com", "Bob", this.pwdHash);
        await().until(() -> this.bob.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);

        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_KO);
        this.server.setRespondingToRequest(false);
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_KO);

        Object[] analyzerLogClientAlice = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@toto.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_KO, ErrorLogMessage.MAIL_NOT_VALID.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_KO, ErrorLogMessage.MAIL_NOT_VALID.getContent()).toString(),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Prout", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_KO, ErrorLogMessage.NAME_NOT_VALID.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_KO, ErrorLogMessage.NAME_NOT_VALID.getContent()).toString(),
                new Request(ProtocolCommand.SIGN_UP, "alice@toto.com", "Prout", this.pwdHash), // invalidity of mail is checked first
                new Request(ProtocolCommand.SIGN_UP_KO, ErrorLogMessage.MAIL_NOT_VALID.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_KO, ErrorLogMessage.MAIL_NOT_VALID.getContent()).toString(),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_KO, ErrorLogMessage.MAIL_ALREADY_TAKEN.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_KO, ErrorLogMessage.MAIL_ALREADY_TAKEN.getContent()).toString(),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()).toString(),
        };

        Object[] analyzerLogClientBob = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.bob.getPublicKey().getEncoded(), this.bob.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Bob", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Bob"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Bob").toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 1).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Bob").toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClientAlice);
        assert lp.checkLog("LogClient_bob@gmail.com.log", analyzerLogClientBob);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    @Test
    public void requestPublicKeyOk() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
    }

    /**
     * Tested error: not responding to request
     */
    @Test
    public void requestPublicKeyKo() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.alice.openConnection();
        this.server.setRespondingToRequest(false);
        this.alice.requestPublicKeyAndIV();
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_KO);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_REQUEST_PUBLIC_KEY_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()).toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
    }

    @Test
    public void signInKo() throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@toto.com", this.pwdHash, true);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_KO);
        this.alice.signIn("alice@gmail.com", "test000", true);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_KO);
        this.server.setRespondingToRequest(false);
        this.alice.signIn("alice@gmail.com", this.pwdHash, true);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_KO);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@toto.com", this.pwdHash, true),
                new Request(ProtocolCommand.SIGN_IN_KO, ErrorLogMessage.COMBINATION_MAIL_PWD_INVALID.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_KO, ErrorLogMessage.COMBINATION_MAIL_PWD_INVALID.getContent()).toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", "test000", true),
                new Request(ProtocolCommand.SIGN_IN_KO, ErrorLogMessage.COMBINATION_MAIL_PWD_INVALID.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_KO, ErrorLogMessage.COMBINATION_MAIL_PWD_INVALID.getContent()).toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, true),
                new Request(ProtocolCommand.SIGN_IN_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()).toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    @Test
    public void signInOk() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    @Test
    public void signOutOk() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        this.alice.signOut();
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_OUT_OK);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.SIGN_OUT),
                new Request(ProtocolCommand.SIGN_OUT_OK),
                ClientState.CONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_OUT_OK).toString()
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }
    
    @Test
    public void signOutKo() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        this.server.setRespondingToRequest(false);
        this.alice.signOut();
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_OUT_KO);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.SIGN_OUT),
                new Request(ProtocolCommand.SIGN_OUT_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()),
                //ClientState.CONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_OUT_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()).toString()
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    /**
     *
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     */
    @Test
    public void annonceFromDomainOk() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        final String annonce1Title = "House";
        final String annonce1Descriptif = "Big house";
        final int annonce1Price = 1000;
        final int annonce1Id = 0;

        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        this.alice.domainList();
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.DOMAINS_LIST_OK);
        this.alice.createAnnonce(Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.CREATE_ANNONCE_OK);
        this.alice.annonceFromDomain(Domain.HOUSE);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_OK);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.DOMAINS_LIST),
                new Request(ProtocolCommand.DOMAINS_LIST_OK, new Object[] {new Domain[] {Domain.HOUSE, Domain.CAR}}),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_DOMAIN_LIST_OK, Arrays.toString(new Domain[]{Domain.HOUSE, Domain.CAR})).toString(),
                new Request(ProtocolCommand.CREATE_ANNONCE, Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price),
                new Request(ProtocolCommand.CREATE_ANNONCE_OK, annonce1Title),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_CREATED_OK, annonce1Title).toString(),
                new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN, Domain.HOUSE),
                // todo: check is error is thrown when "alice" replace with something else
                new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN_OK, new Object[] {new Annonce[] {new Annonce("Alice", Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price, annonce1Id)}}),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_FROM_DOMAIN_OK, Arrays.toString(new Annonce[]{new Annonce("Alice", Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price, annonce1Id)})).toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CREATE_ANNONCE, "alice@gmail.com", annonce1Title, annonce1Descriptif, Domain.HOUSE, annonce1Price, annonce1Id).toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    /**
     *
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     */
    @Test
    public void annonceFromDomainKo() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        // todo: remove domainList() from all tets if it is not necessary
        this.alice.annonceFromDomain(Domain.HOUSE);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_KO);
        this.server.setRespondingToRequest(false);
        this.alice.annonceFromDomain(Domain.HOUSE);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_KO);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN, Domain.HOUSE),
                new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN_KO, ErrorLogMessage.NO_ANNONCE_IN_THAT_DOMAIN.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_FROM_DOMAIN_KO, ErrorLogMessage.NO_ANNONCE_IN_THAT_DOMAIN.getContent()).toString(),
                new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN, Domain.HOUSE),
                new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_FROM_DOMAIN_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()).toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    @Test
    public void createAnnonceOk() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        final String annonce1Title = "House";
        final String annonce1Descriptif = "Big house";
        final int annonce1Price = 1000;
        final int annonce1Id = 0;

        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        this.alice.domainList();
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.DOMAINS_LIST_OK);
        this.alice.createAnnonce(Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.CREATE_ANNONCE_OK);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.DOMAINS_LIST),
                new Request(ProtocolCommand.DOMAINS_LIST_OK, new Object[] {new Domain[] {Domain.HOUSE, Domain.CAR}}),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_DOMAIN_LIST_OK, Arrays.toString(new Domain[] {Domain.HOUSE, Domain.CAR})).toString(),
                new Request(ProtocolCommand.CREATE_ANNONCE, Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price),
                new Request(ProtocolCommand.CREATE_ANNONCE_OK, annonce1Title),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_CREATED_OK, annonce1Title).toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CREATE_ANNONCE, "alice@gmail.com", annonce1Title, annonce1Descriptif, Domain.HOUSE, annonce1Price, annonce1Id).toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    @Test
    public void updateAnnonceOk() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        final String annonce1Title = "House";
        final String annonce1TitleUpdated = "Beautiful House";
        final String annonce1Descriptif = "Big house";
        final String annonce1DescriptifUpdated = "Very big house";
        final int annonce1Price = 1000;
        final int annonce1PriceUpdated = 1200;
        final int annonce1Id = 0;

        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        this.alice.createAnnonce(Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.CREATE_ANNONCE_OK);
        this.alice.updateAnnonce(annonce1TitleUpdated, annonce1DescriptifUpdated, annonce1PriceUpdated, annonce1Id);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.UPDATE_ANNONCE_OK);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.CREATE_ANNONCE, Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price),
                new Request(ProtocolCommand.CREATE_ANNONCE_OK, annonce1Title),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_CREATED_OK, annonce1Title).toString(),
                new Request(ProtocolCommand.UPDATE_ANNONCE, annonce1TitleUpdated, annonce1DescriptifUpdated, annonce1PriceUpdated, annonce1Id),
                new Request(ProtocolCommand.UPDATE_ANNONCE_OK),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_UPDATED_OK).toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CREATE_ANNONCE, "alice@gmail.com", annonce1Title, annonce1Descriptif, Domain.HOUSE, annonce1Price, annonce1Id).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_UPDATE_ANNONCE, "alice@gmail.com", annonce1TitleUpdated, annonce1DescriptifUpdated, Domain.HOUSE, annonce1PriceUpdated, annonce1Id).toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    @Test
    public void updateAnnonceKo() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InterruptedException {
        final String annonce1Title = "House";
        final String annonce1TitleUpdated = "Beautiful House";
        final String annonce1Descriptif = "Big house";
        final String annonce1DescriptifUpdated = "Very big house";
        final int annonce1Price = 1000;
        final int annonce1PriceUpdated = 1200;
        final int annonce1Id = 0;

        this.bob = new Client("bob@gmail.com", "Bob", false);

        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);

        this.bob.openConnection();
        this.bob.requestPublicKeyAndIV();
        await().until(fieldIn(this.bob).ofType(SecretKey.class), notNullValue());
        this.bob.signUp("bob@gmail.com", "Bob", this.pwdHash);
        await().until(() -> this.bob.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.bob.signIn("bob@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);

        this.alice.createAnnonce(Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.CREATE_ANNONCE_OK);
        this.bob.updateAnnonce(annonce1TitleUpdated, annonce1DescriptifUpdated, annonce1PriceUpdated, annonce1Id);
        await().until(() -> this.bob.getLastRequest().getCommand() == ProtocolCommand.UPDATE_ANNONCE_KO);
        this.server.setRespondingToRequest(false);
        this.alice.updateAnnonce(annonce1TitleUpdated, annonce1DescriptifUpdated, annonce1PriceUpdated, annonce1Id);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.UPDATE_ANNONCE_KO);

        Object[] analyzerLogClientAlice = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.CREATE_ANNONCE, Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price),
                new Request(ProtocolCommand.CREATE_ANNONCE_OK, annonce1Title),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_CREATED_OK, annonce1Title).toString(),
                new Request(ProtocolCommand.UPDATE_ANNONCE, annonce1TitleUpdated, annonce1DescriptifUpdated, annonce1PriceUpdated, annonce1Id),
                new Request(ProtocolCommand.UPDATE_ANNONCE_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_UPDATED_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()).toString(),
        };

        Object[] analyzerLogClientBob = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.bob.getPublicKey().getEncoded(), this.bob.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "bob@gmail.com", "Bob", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "bob@gmail.com", "Bob"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "bob@gmail.com", "Bob").toString(),
                new Request(ProtocolCommand.SIGN_IN, "bob@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Bob"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "bob@gmail.com").toString(),
                new Request(ProtocolCommand.UPDATE_ANNONCE, annonce1TitleUpdated, annonce1DescriptifUpdated, annonce1PriceUpdated, annonce1Id),
                new Request(ProtocolCommand.UPDATE_ANNONCE_KO, ErrorLogMessage.NOT_OWNER.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_UPDATED_KO, ErrorLogMessage.NOT_OWNER.getContent()).toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 1).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "bob@gmail.com", "Bob").toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CREATE_ANNONCE, "alice@gmail.com", annonce1Title, annonce1Descriptif, Domain.HOUSE, annonce1Price, annonce1Id).toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClientAlice);
        assert lp.checkLog("LogClient_bob@gmail.com.log", analyzerLogClientBob);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    @Test
    public void removeAnnonceOk() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        final String annonce1Title = "House";
        final String annonce1Descriptif = "Big house";
        final int annonce1Price = 1000;
        final int annonce1Id = 0;

        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        this.alice.createAnnonce(Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.CREATE_ANNONCE_OK);
        this.alice.removeAnnonce(annonce1Id);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.REMOVE_ANNONCE_OK);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.CREATE_ANNONCE, Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price),
                new Request(ProtocolCommand.CREATE_ANNONCE_OK, annonce1Title),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_CREATED_OK, annonce1Title).toString(),
                new Request(ProtocolCommand.REMOVE_ANNONCE, annonce1Id),
                new Request(ProtocolCommand.REMOVE_ANNONCE_OK),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_REMOVE_ANNONCE_OK).toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CREATE_ANNONCE, "alice@gmail.com", annonce1Title, annonce1Descriptif, Domain.HOUSE, annonce1Price, annonce1Id).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_REMOVE_ANNONCE, "alice@gmail.com", annonce1Title, annonce1Descriptif, Domain.HOUSE, annonce1Price, annonce1Id).toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    @Test
    public void removeAnnonceKo() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InterruptedException {
        final String annonce1Title = "House";
        final String annonce1Descriptif = "Big house";
        final int annonce1Price = 1000;
        final int annonce1Id = 0;

        this.bob = new Client("bob@gmail.com", "Bob", false);

        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);

        this.bob.openConnection();
        this.bob.requestPublicKeyAndIV();
        await().until(fieldIn(this.bob).ofType(SecretKey.class), notNullValue());
        this.bob.signUp("bob@gmail.com", "Bob", this.pwdHash);
        await().until(() -> this.bob.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.bob.signIn("bob@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);

        this.alice.createAnnonce(Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.CREATE_ANNONCE_OK);
        this.bob.removeAnnonce(annonce1Id);
        await().until(() -> this.bob.getLastRequest().getCommand() == ProtocolCommand.REMOVE_ANNONCE_KO);
        this.server.setRespondingToRequest(false);
        this.alice.removeAnnonce(annonce1Id);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.REMOVE_ANNONCE_KO);

        Object[] analyzerLogClientAlice = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.CREATE_ANNONCE, Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price),
                new Request(ProtocolCommand.CREATE_ANNONCE_OK, annonce1Title),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_CREATED_OK, annonce1Title).toString(),
                new Request(ProtocolCommand.REMOVE_ANNONCE, annonce1Id),
                new Request(ProtocolCommand.REMOVE_ANNONCE_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_REMOVE_ANNONCE_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()).toString(),
        };

        Object[] analyzerLogClientBob = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.bob.getPublicKey().getEncoded(), this.bob.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "bob@gmail.com", "Bob", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "bob@gmail.com", "Bob"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "bob@gmail.com", "Bob").toString(),
                new Request(ProtocolCommand.SIGN_IN, "bob@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Bob"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "bob@gmail.com").toString(),
                new Request(ProtocolCommand.REMOVE_ANNONCE, annonce1Id),
                new Request(ProtocolCommand.REMOVE_ANNONCE_KO, ErrorLogMessage.NOT_OWNER.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_REMOVE_ANNONCE_KO, ErrorLogMessage.NOT_OWNER.getContent()).toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 1).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "bob@gmail.com", "Bob").toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CREATE_ANNONCE, "alice@gmail.com", annonce1Title, annonce1Descriptif, Domain.HOUSE, annonce1Price, annonce1Id).toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClientAlice);
        assert lp.checkLog("LogClient_bob@gmail.com.log", analyzerLogClientBob);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    @Test
    public void createAnnonceKo() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        final String annonce1Title = "House";
        final String annonce1Descriptif = "Big house";
        final int annonce1Price = 1000;

        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        this.alice.domainList();
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.DOMAINS_LIST_OK);
        this.server.setRespondingToRequest(false);
        this.alice.createAnnonce(Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.CREATE_ANNONCE_KO);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.DOMAINS_LIST),
                new Request(ProtocolCommand.DOMAINS_LIST_OK, (Object) new Domain[]{Domain.HOUSE, Domain.CAR}),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_DOMAIN_LIST_OK, Arrays.toString(new Domain[]{Domain.HOUSE, Domain.CAR})).toString(),
                new Request(ProtocolCommand.CREATE_ANNONCE, Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price),
                new Request(ProtocolCommand.CREATE_ANNONCE_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_CREATED_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()).toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    @Test
    public void domainListOk() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        this.alice.domainList();
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.DOMAINS_LIST_OK);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[]{this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.DOMAINS_LIST),
                new Request(ProtocolCommand.DOMAINS_LIST_OK, (Object) new Domain[]{Domain.HOUSE, Domain.CAR}),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_DOMAIN_LIST_OK, Arrays.toString(new Domain[]{Domain.HOUSE, Domain.CAR})).toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    @Test
    public void domainListKo() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", this.pwdHash);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", this.pwdHash, false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        this.server.setRespondingToRequest(false);
        this.alice.domainList();
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.DOMAINS_LIST_KO);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[]{this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", this.pwdHash),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", this.pwdHash, false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.DOMAINS_LIST),
                new Request(ProtocolCommand.DOMAINS_LIST_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_DOMAIN_LIST_KO, ErrorLogMessage.NOT_RESPONDING_TO_REQUEST.getContent()).toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
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

    //todo: creer test qui teste les erreurs (mauvaise conbinaison nom/pwd -> la bonne erreur est renvoyée)

}
