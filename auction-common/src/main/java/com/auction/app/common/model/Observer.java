package com.auction.app.common.model;

import com.auction.app.common.dto.NetworkMessage;

public interface Observer {
    void update(NetworkMessage message);
}
