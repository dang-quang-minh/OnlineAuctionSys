package com.auction.app.server.network;

import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.server.service.AuctionManager;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerNetworkSocketTest {

    private static final int SOCKET_TIMEOUT_MILLIS =
            (int) TimeUnit.SECONDS.toMillis(3);

    @Test
    void routesRequestsToTheClientThatSentThem() throws Exception {
        try (ServerNetwork server = new ServerNetwork(0)) {
            server.registerHandler(
                    "PING",
                    request -> new NetworkMessage("PONG", request.getPayload())
            );
            server.start();

            try (TestClient first = connect(server);
                 TestClient second = connect(server)) {

                first.send(new NetworkMessage("PING", "from-first"));
                assertMessage(first.receive(), "PONG", "from-first");

                second.send(new NetworkMessage("PING", "from-second"));
                assertMessage(second.receive(), "PONG", "from-second");
            }
        }
    }

    @Test
    void serverContinuesServingRemainingClientAfterDisconnect() throws Exception {
        try (ServerNetwork server = new ServerNetwork(0)) {
            server.registerHandler(
                    "PING",
                    request -> new NetworkMessage("PONG", request.getPayload())
            );
            server.start();

            try (TestClient disconnected = connect(server);
                 TestClient remaining = connect(server)) {

                disconnected.close();

                remaining.send(new NetworkMessage("PING", "still-alive"));
                assertMessage(remaining.receive(), "PONG", "still-alive");
            }
        }
    }

    @Test
    void broadcastsEventsToEveryConnectedClient() throws Exception {
        try (ServerNetwork server = new ServerNetwork(0)) {
            server.registerHandler(
                    "PING",
                    request -> new NetworkMessage("PONG", request.getPayload())
            );
            server.start();

            try (TestClient first = connect(server);
                 TestClient second = connect(server)) {

                // Gửi request để mỗi client đã kết nối và nhận được phản hồi.
                first.send(new NetworkMessage("PING", "ready-first"));
                assertMessage(first.receive(), "PONG", "ready-first");

                second.send(new NetworkMessage("PING", "ready-second"));
                assertMessage(second.receive(), "PONG", "ready-second");

                NetworkMessage event =
                        new NetworkMessage(NetworkMessage.NEW_BID, "test-event");

                AuctionManager.getInstance().notifyObservers(event);

                // Cả hai client đều phải nhận cùng một event broadcast.
                assertMessage(first.receive(), NetworkMessage.NEW_BID, "test-event");
                assertMessage(second.receive(), NetworkMessage.NEW_BID, "test-event");
            }
        }
    }

    private static void assertMessage(
            NetworkMessage actual,
            String expectedType,
            String expectedPayload) {

        assertEquals(expectedType, actual.getType(), "Message type không đúng.");
        assertEquals(expectedPayload, actual.getPayload(), "Message payload không đúng.");
    }

    private static TestClient connect(ServerNetwork server) throws IOException {
        return new TestClient(server.getPort());
    }

    private static final class TestClient implements AutoCloseable {

        private final Socket socket;
        private final ObjectOutputStream output;
        private final ObjectInputStream input;

        private TestClient(int port) throws IOException {
            socket = new Socket("localhost", port);
            socket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);

            // Tạo output stream và flush trước khi tạo input stream,
            // tránh hai đầu Socket chờ nhau trong lúc khởi tạo.
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();

            input = new ObjectInputStream(socket.getInputStream());
        }

        private void send(NetworkMessage message) throws IOException {
            output.writeObject(message);
            output.flush();
            output.reset();
        }

        private NetworkMessage receive()
                throws IOException, ClassNotFoundException {

            Object value = input.readObject();

            if (!(value instanceof NetworkMessage message)) {
                throw new IOException(
                        "Server trả về object không phải NetworkMessage."
                );
            }

            return message;
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}