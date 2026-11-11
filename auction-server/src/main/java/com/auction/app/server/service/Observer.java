package com.auction.app.server.service;

interface Observer {
    public void update(NetworkMessage message);
}
