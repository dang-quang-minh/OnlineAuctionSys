package com.auction.app.server.service;

import com.auction.app.common.dto.NetworkMessage;

interface Subject {

    void registerObserver(Observer o);

    void removeObserver(Observer o);

    void notifyObservers(NetworkMessage message);

}