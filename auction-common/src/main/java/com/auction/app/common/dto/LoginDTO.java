package com.auction.app.common.dto;

import java.io.Serializable;

public record LoginDTO(String username, String password) implements Serializable {
}