package com.auction.app.server.network;

import com.auction.app.common.dto.NetworkMessage;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Routes a client request to the handler registered for its message type. */
public final class MessageDispatcher {

    private final Map<String, Function<NetworkMessage, NetworkMessage>> handlers =
            new ConcurrentHashMap<>();

    /**
     * Registers one request type. Registration is intended to happen during server setup.
     *
     * @param type request message type
     * @param handler handler that returns the response for the requesting client
     */
    public void register(
            String type,
            Function<NetworkMessage, NetworkMessage> handler) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Message type không được để trống.");
        }
        Objects.requireNonNull(handler, "handler");
        if (handlers.putIfAbsent(type, handler) != null) {
            throw new IllegalArgumentException("Handler đã được đăng ký cho message: " + type);
        }
    }

    /**
     * Replaces a default route before the server starts, for example when an
     * AuctionHandler or ProductCatalogHandler is supplied by another job.
     *
     * @param type request message type
     * @param handler replacement handler
     */
    public void replace(
            String type,
            Function<NetworkMessage, NetworkMessage> handler) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Message type không được để trống.");
        }
        handlers.put(type, Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Dispatches a request and converts handler failures into an ERROR response.
     *
     * @param request client request
     * @return response for the client that sent the request
     */
    public NetworkMessage dispatch(NetworkMessage request) {
        if (request == null) {
            return error("Message không được để trống.");
        }

        Function<NetworkMessage, NetworkMessage> handler = handlers.get(request.getType());
        if (handler == null) {
            return error("Loại message không được hỗ trợ: " + request.getType());
        }

        try {
            NetworkMessage response = handler.apply(request);
            return response == null ? error("Handler không tạo phản hồi.") : response;
        } catch (RuntimeException exception) {
            return error(exception.getMessage());
        }
    }

    private static NetworkMessage error(String message) {
        return new NetworkMessage(
                NetworkMessage.ERROR,
                message == null || message.isBlank() ? "Yêu cầu bị từ chối." : message);
    }
}
