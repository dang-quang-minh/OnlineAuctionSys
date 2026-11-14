package com.auction.app.client.network;

import com.auction.app.common.dto.NetworkMessage;

/**
 * Interface lắng nghe các gói tin từ Server gửi về phía Client.
 * Giúp tách biệt tầng mạng (client.network) khỏi tầng điều khiển giao diện (client.controller).
 */
@FunctionalInterface
public interface MessageListener {

    /**
     * Được gọi ngay khi Client nhận được một NetworkMessage hợp lệ từ Server.
     *
     * @param message Gói tin nhận được từ luồng Socket
     */
    void onMessageReceived(NetworkMessage message);
}
