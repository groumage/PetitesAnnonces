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
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.logging.LogManager;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.fieldIn;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class Scenario {

    Server server;
    Client alice;

    @Before
    public void initServerAndClient() throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InterruptedException, InvalidKeySpecException {
        this.server = new Server(true, false);
        this.alice = new Client("alice@gmail.com", "Alice", false);
        new Thread(this.server).start();
    }

    /**
     * server close connection -> client open connection
     * @throws IOException
     */
    @Test
    public void scenarioConnectWhenServerConnectionIsClosed() throws IOException {
        this.server.closeConnection(); // serversocket is open by default
        this.alice.openConnection();

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_UNREACHABLE_SERVER).toString(),
        };
        Object[] analyzerLogServer = {};

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    // todo: add a domain to the created annonce and update the title, the domain, the price and the description
    /**
     * client open connection -> sign up -> sign in -> create annonce -> get annonce -> update annonce -> get annonce -> create annonce -> get annonce -> remove annonce -> get annonce
     * @throws IOException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws NoSuchAlgorithmException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     */
    @Test
    public void scenarioCreateUpdateRemoveAnnonce() throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        final String annonce1Title = "House";
        final String annonce1TitleUpdated = "Beautiful House";
        final String annonce1Descriptif = "Big house";
        final String annonce1DescriptifUpdated = "Very big house";
        final int annonce1Price = 1000;
        final int annonce1PriceUpdated = 1200;
        final int annonce1Id = 0;

        final String annonce2Title = "Palace";
        final String annonce2Descriptif = "Palace house";
        final int annonce2Price = 5000;
        final int annonce2Id = 1;

        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", "test");
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", "test", false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        this.alice.domainList();
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.DOMAINS_LIST_OK);
        this.alice.createAnnonce(Domain.HOUSE, annonce1Title, annonce1Descriptif, annonce1Price);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.CREATE_ANNONCE_OK);
        this.alice.annonceFromDomain(Domain.HOUSE);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_OK);
        this.alice.updateAnnonce(annonce1TitleUpdated, annonce1DescriptifUpdated, annonce1PriceUpdated, annonce1Id);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.UPDATE_ANNONCE_OK);
        this.alice.annonceFromDomain(Domain.HOUSE);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_OK);
        this.alice.createAnnonce(Domain.HOUSE, annonce2Title, annonce2Descriptif, annonce2Price);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.CREATE_ANNONCE_OK);
        this.alice.annonceFromDomain(Domain.HOUSE);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_OK);
        this.alice.removeAnnonce(annonce1Id);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.REMOVE_ANNONCE_OK);
        this.alice.annonceFromDomain(Domain.HOUSE);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.ANNONCE_FROM_DOMAIN_OK);

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, this.alice.getPublicKey().getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", "test"),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", "test", false),
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
                new Request(ProtocolCommand.UPDATE_ANNONCE, annonce1TitleUpdated, annonce1DescriptifUpdated, annonce1PriceUpdated, annonce1Id),
                //todo: thow error if more argument than expected
                new Request(ProtocolCommand.UPDATE_ANNONCE_OK),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_UPDATED_OK).toString(),
                new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN, Domain.HOUSE),
                new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN_OK, new Object[] {new Annonce[] {new Annonce("Alice", Domain.HOUSE, annonce1TitleUpdated, annonce1DescriptifUpdated, annonce1PriceUpdated, annonce1Id)}}),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_FROM_DOMAIN_OK, Arrays.toString(new Annonce[]{new Annonce("Alice", Domain.HOUSE, annonce1TitleUpdated, annonce1DescriptifUpdated, annonce1PriceUpdated, 0)})).toString(),
                new Request(ProtocolCommand.CREATE_ANNONCE, Domain.HOUSE, annonce2Title, annonce2Descriptif, annonce2Price),
                new Request(ProtocolCommand.CREATE_ANNONCE_OK, annonce2Title),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_CREATED_OK, annonce2Title).toString(),
                new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN, Domain.HOUSE),
                new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN_OK, new Object[] {new Annonce[] {new Annonce("Alice", Domain.HOUSE, annonce1TitleUpdated, annonce1DescriptifUpdated, annonce1PriceUpdated, annonce1Id), new Annonce("Alice", Domain.HOUSE, annonce2Title, annonce2Descriptif, annonce2Price, annonce2Id)}}),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_FROM_DOMAIN_OK, Arrays.toString(new Annonce[]{new Annonce("Alice", Domain.HOUSE, annonce1TitleUpdated, annonce1DescriptifUpdated, annonce1PriceUpdated, annonce1Id), new Annonce("Alice", Domain.HOUSE, annonce2Title, annonce2Descriptif, annonce2Price, annonce2Id)})).toString(),
                new Request(ProtocolCommand.REMOVE_ANNONCE, annonce1Id),
                new Request(ProtocolCommand.REMOVE_ANNONCE_OK),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_REMOVE_ANNONCE_OK).toString(),
                new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN, Domain.HOUSE),
                new Request(ProtocolCommand.ANNONCE_FROM_DOMAIN_OK, new Object[] {new Annonce[] {new Annonce("Alice", Domain.HOUSE, annonce2Title, annonce2Descriptif, annonce2Price, annonce2Id)}}),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_ANNONCE_FROM_DOMAIN_OK, Arrays.toString(new Annonce[]{new Annonce("Alice", Domain.HOUSE, annonce2Title, annonce2Descriptif, annonce2Price, annonce2Id)})).toString(),
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CREATE_ANNONCE, "alice@gmail.com", annonce1Title, annonce1Descriptif, Domain.HOUSE, annonce1Price, annonce1Id).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_UPDATE_ANNONCE, "alice@gmail.com", annonce1TitleUpdated, annonce1DescriptifUpdated, Domain.HOUSE, annonce1PriceUpdated, annonce1Id).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CREATE_ANNONCE, "alice@gmail.com", annonce2Title, annonce2Descriptif, Domain.HOUSE, annonce2Price, annonce2Id).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_REMOVE_ANNONCE, "alice@gmail.com", annonce1TitleUpdated, annonce1DescriptifUpdated, Domain.HOUSE, annonce1PriceUpdated, annonce1Id).toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }

    // todo: tester les erreurs retourné par les fct de création, modification et suppression d'annonces
    /**
     * client open connection -> sign up -> sign in -> receive domain -> sign out -> client close connection -> client open connection -> sign in -> receive domain -> sign out -> client close connection
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws ClassNotFoundException
     */
    @Test
    public void scenarioOpenConnectionAfterCloseConnection() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, ClassNotFoundException {
        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signUp("alice@gmail.com", "Alice", "test");
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_UP_OK);
        this.alice.signIn("alice@gmail.com", "test", true);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        this.alice.signOut();
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_OUT_OK);
        PublicKey memPk1 = this.alice.getPublicKey();
        this.alice.closeConnection();
        await().until(fieldIn(this.alice).ofType(ClientState.class), equalTo(ClientState.DISCONNECTED));
        this.alice.openConnection();
        this.alice.requestPublicKeyAndIV();
        await().until(fieldIn(this.alice).ofType(SecretKey.class), notNullValue());
        this.alice.signIn("alice@gmail.com", "test", false);
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_IN_OK);
        this.alice.signOut();
        await().until(() -> this.alice.getLastRequest().getCommand() == ProtocolCommand.SIGN_OUT_OK);
        PublicKey memPk2 = this.alice.getPublicKey();
        this.alice.closeConnection();
        await().until(fieldIn(this.alice).ofType(ClientState.class), equalTo(ClientState.DISCONNECTED));

        Object[] analyzerLogClient = {
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, memPk1.getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_UP, "alice@gmail.com", "Alice", "test"),
                new Request(ProtocolCommand.SIGN_UP_OK, "alice@gmail.com", "Alice"),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_UP_OK, "alice@gmail.com", "Alice").toString(),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", "test", true),
                new Request(ProtocolCommand.DOMAINS_LIST_OK, new Object[] {new Domain[]{Domain.HOUSE, Domain.CAR}}),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_DOMAIN_LIST_OK, Arrays.toString(new Domain[]{Domain.HOUSE, Domain.CAR})).toString(),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.SIGN_OUT),
                new Request(ProtocolCommand.SIGN_OUT_OK),
                ClientState.CONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_OUT_OK).toString(),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_CLOSED).toString(),
                ClientState.DISCONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_OPEN).toString(),
                ClientState.CONNECTED,
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_AND_SEND_IV, memPk2.getEncoded(), this.alice.generate16Bytes()),
                new Request(ProtocolCommand.REQUEST_EXCHANGE_SERVER_PUBLIC_KEY_OK, new Object[] {this.server.getPk().getEncoded()}),
                new Request(ProtocolCommand.SIGN_IN, "alice@gmail.com", "test", false),
                new Request(ProtocolCommand.SIGN_IN_OK, "Alice"),
                ClientState.LOGGED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_IN_OK, "alice@gmail.com").toString(),
                new Request(ProtocolCommand.SIGN_OUT),
                new Request(ProtocolCommand.SIGN_OUT_OK),
                ClientState.CONNECTED,
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SIGN_OUT_OK).toString(),
                new InternalLogMessage(TokenInternalLogMessage.CLIENT_LOG_SOCKET_CLOSED).toString(),
                ClientState.DISCONNECTED,
        };

        Object[] analyzerLogServer = {
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_CREATED, "alice@gmail.com", "Alice").toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER, 0).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER_CREATED, 1).toString(),
                new InternalLogMessage(TokenInternalLogMessage.SERVER_LOG_CLIENT_HANDLER, 1).toString(),
        };

        LogParser lp = new LogParser();
        assert lp.checkLog("LogClient_alice@gmail.com.log", analyzerLogClient);
        assert lp.checkLog("LogServer.log", analyzerLogServer);
    }


    @After
    public void stopServerAndStopClient() throws IOException {
        this.alice.stopProcess();
        this.server.stopProcess();
        LogManager logManager = LogManager.getLogManager();
        logManager.reset();
    }
}
