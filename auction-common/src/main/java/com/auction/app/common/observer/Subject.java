package com.auction.app.common.observer;

import com.auction.app.common.dto.NetworkMessage;

public interface Subject {

    void registerObserver(Observer o);

    void removeObserver(Observer o);

    void notifyObservers(NetworkMessage message);

}