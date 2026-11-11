package com.auction.app.server.service;

interface Subject {
    void registerObserver(Observer o);
    void removeObserver(Observer o);
    void notifyObservers(NetworkMessage message);
}