package com.auction.app.common.observer;

import com.auction.app.common.dto.NetworkMessage;

public interface Observer {

    void update(NetworkMessage message);

}
