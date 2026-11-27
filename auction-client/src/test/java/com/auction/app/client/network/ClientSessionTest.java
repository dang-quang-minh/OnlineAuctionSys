package com.auction.app.client.network;

import com.auction.app.common.dto.LoginDTO;
import com.auction.app.common.dto.LoginResultDTO;
import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.dto.RegisterRequestDTO;
import com.auction.app.common.dto.RegisterResultDTO;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientSessionTest {

    @Test
    void sendsLoginThroughClientNetworkAndReceivesServerResponse() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CountDownLatch serverReady = new CountDownLatch(1);
            Thread serverThread = new Thread(() -> serveLogin(server, serverReady));
            serverThread.start();
            assertTrue(serverReady.await(2, TimeUnit.SECONDS));

            System.setProperty("auction.server.host", "localhost");
            System.setProperty("auction.server.port",
                    Integer.toString(server.getLocalPort()));
            CountDownLatch responseReady = new CountDownLatch(1);
            AtomicReference<LoginResultDTO> response = new AtomicReference<>();
            ClientSession session = ClientSession.getInstance();
            try {
                session.login("demo", "demo", result -> {
                    response.set(result);
                    responseReady.countDown();
                }, message -> responseReady.countDown());
                assertTrue(responseReady.await(2, TimeUnit.SECONDS));
                assertEquals(new LoginResultDTO(true, "ok"), response.get());
            } finally {
                session.disconnect();
                System.clearProperty("auction.server.host");
                System.clearProperty("auction.server.port");
            }
        }
    }

    @Test
    void sendsRegisterThroughClientNetworkAndReceivesServerResponse() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CountDownLatch serverReady = new CountDownLatch(1);
            Thread serverThread = new Thread(() -> serveRegister(server, serverReady));
            serverThread.start();
            assertTrue(serverReady.await(2, TimeUnit.SECONDS));

            System.setProperty("auction.server.host", "localhost");
            System.setProperty("auction.server.port",
                    Integer.toString(server.getLocalPort()));
            CountDownLatch responseReady = new CountDownLatch(1);
            AtomicReference<RegisterResultDTO> response = new AtomicReference<>();
            ClientSession session = ClientSession.getInstance();
            try {
                session.register(new RegisterRequestDTO("new_user", "n@test.com", "pass", "BIDDER"), result -> {
                    response.set(result);
                    responseReady.countDown();
                }, message -> responseReady.countDown());
                assertTrue(responseReady.await(2, TimeUnit.SECONDS));
                assertEquals(new RegisterResultDTO(true, "registered"), response.get());
            } finally {
                session.disconnect();
                System.clearProperty("auction.server.host");
                System.clearProperty("auction.server.port");
            }
        }
    }

    private static void serveRegister(
            ServerSocket server,
            CountDownLatch serverReady) {
        serverReady.countDown();
        try (Socket socket = server.accept()) {
            ObjectOutputStream output =
                    new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            ObjectInputStream input =
                    new ObjectInputStream(socket.getInputStream());
            NetworkMessage request = (NetworkMessage) input.readObject();
            assertEquals(NetworkMessage.REGISTER, request.getType());
            assertTrue(request.getPayload() instanceof RegisterRequestDTO);
            output.writeObject(new NetworkMessage(
                    NetworkMessage.REGISTER_RESULT, new RegisterResultDTO(true, "registered")));
            output.flush();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void serveLogin(
            ServerSocket server,
            CountDownLatch serverReady) {
        serverReady.countDown();
        try (Socket socket = server.accept()) {
            ObjectOutputStream output =
                    new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            ObjectInputStream input =
                    new ObjectInputStream(socket.getInputStream());
            NetworkMessage request = (NetworkMessage) input.readObject();
            assertEquals(NetworkMessage.LOGIN, request.getType());
            assertTrue(request.getPayload() instanceof LoginDTO);
            output.writeObject(new NetworkMessage(
                    NetworkMessage.LOGIN_RESULT, new LoginResultDTO(true, "ok")));
            output.flush();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
