package com.auction.app.server.network;

import com.auction.app.common.dto.NetworkMessage;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageDispatcherTest {

    @Test
    void registeredRouteReceivesRequestAndReturnsResponse() {
        MessageDispatcher dispatcher = new MessageDispatcher();
        NetworkMessage request = new NetworkMessage("PING", "hello");
        NetworkMessage expectedResponse = new NetworkMessage("PONG", "ok");
        AtomicReference<NetworkMessage> receivedRequest = new AtomicReference<>();

        dispatcher.register("PING", message -> {
            receivedRequest.set(message);
            return expectedResponse;
        });

        NetworkMessage actualResponse = dispatcher.dispatch(request);

        assertSame(request, receivedRequest.get());
        assertSame(expectedResponse, actualResponse);
    }

    @Test
    void unknownMessageTypeReturnsError() {
        MessageDispatcher dispatcher = new MessageDispatcher();

        NetworkMessage response =
                dispatcher.dispatch(new NetworkMessage("UNKNOWN", "request"));

        assertEquals(NetworkMessage.ERROR, response.getType());
        assertEquals(
                "Loại message không được hỗ trợ: UNKNOWN",
                response.getPayload()
        );
    }

    @Test
    void handlerFailureReturnsError() {
        MessageDispatcher dispatcher = new MessageDispatcher();
        dispatcher.register("BROKEN", message -> {
            throw new IllegalStateException("handler failed");
        });

        NetworkMessage response =
                dispatcher.dispatch(new NetworkMessage("BROKEN", "request"));

        assertEquals(NetworkMessage.ERROR, response.getType());
        assertEquals("handler failed", response.getPayload());
    }

    @Test
    void duplicateRouteRegistrationIsRejected() {
        MessageDispatcher dispatcher = new MessageDispatcher();
        dispatcher.register("PING", message -> new NetworkMessage("PONG", "first"));

        assertThrows(
                IllegalArgumentException.class,
                () -> dispatcher.register(
                        "PING",
                        message -> new NetworkMessage("PONG", "second")
                )
        );
    }
}