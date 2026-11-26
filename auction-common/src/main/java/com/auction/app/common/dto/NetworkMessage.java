package com.auction.app.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class NetworkMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_RESULT = "LOGIN_RESULT";
    public static final String AUCTION_LIST = "AUCTION_LIST";
    public static final String PLACE_BID = "PLACE_BID";
    public static final String NEW_BID = "NEW_BID";
    public static final String AUCTION_ENDED = "AUCTION_ENDED";
    public static final String ERROR = "ERROR";

    private final String type;
    private final Serializable payload;

    public NetworkMessage(String type, Serializable payload) {
        this.type = Objects.requireNonNull(type, "type");
        this.payload = Objects.requireNonNull(payload, "payload");
    }

    public String getType() {
        return type;
    }

    public Serializable getPayload() {
        return payload;
    }
}
