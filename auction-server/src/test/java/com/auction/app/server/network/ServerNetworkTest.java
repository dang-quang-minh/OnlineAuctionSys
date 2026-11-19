package com.auction.app.server.network;

import com.auction.app.common.dto.LoginDTO;
import com.auction.app.common.dto.LoginResultDTO;
import com.auction.app.common.dto.NetworkMessage;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerNetworkTest {

    @Test
    void acceptsSerializedLoginMessageAndReturnsResult() throws Exception {
        try (ServerNetwork server = new ServerNetwork(0)) {
            server.start();

            try (Socket socket = new Socket("localhost", server.getPort())) {
                ObjectOutputStream output =
                        new ObjectOutputStream(socket.getOutputStream());
                output.flush();
                ObjectInputStream input =
                        new ObjectInputStream(socket.getInputStream());

                output.writeObject(new NetworkMessage(
                        NetworkMessage.LOGIN, new LoginDTO("demo", "demo")));
                output.flush();

                Object response = input.readObject();
                assertTrue(response instanceof NetworkMessage);
                NetworkMessage message = (NetworkMessage) response;
                assertEquals(NetworkMessage.LOGIN_RESULT, message.getType());
                assertEquals(new LoginResultDTO(true, "Đăng nhập thành công."),
                        message.getPayload());
            }
        }
    }
}
