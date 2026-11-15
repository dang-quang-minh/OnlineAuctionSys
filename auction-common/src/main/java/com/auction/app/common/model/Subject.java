package com.auction.app.common.model;

import com.auction.app.common.dto.NetworkMessage;

public interface Subject {
    void registerObserver(Observer observer);

    void removeObserver(Observer observer);

    void notifyObservers(NetworkMessage message);
}
