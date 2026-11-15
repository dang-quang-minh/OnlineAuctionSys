package com.auction.app.common.dto;

import java.io.Serializable;
import java.util.Objects;

public final class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String type;
    private final Serializable data;

    public NetworkMessage(String type, Serializable data) {
        this.type = Objects.requireNonNull(type, "type");
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public Serializable getData() {
        return data;
    }
}
