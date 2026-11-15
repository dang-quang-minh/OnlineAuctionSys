package com.auction.app.server.service;

import com.auction.app.common.dto.NetworkMessage;

interface Observer {

    void update(NetworkMessage message);

}
