package com.auction.app.server.network;

import com.auction.app.common.dto.LoginDTO;
import com.auction.app.common.dto.LoginResultDTO;
import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.dto.RegisterRequestDTO;
import com.auction.app.common.dto.RegisterResultDTO;
import com.auction.app.server.service.AccountService;
import java.util.Objects;

/**
 * Điều phối xử lý các thông điệp tài khoản (LOGIN, REGISTER).
 */
public class AccountHandler {

    private final AccountService accountService;

    public AccountHandler(AccountService accountService) {
        this.accountService = Objects.requireNonNull(accountService, "accountService");
    }

    /**
     * Xử lý thông điệp liên quan đến tài khoản.
     */
    public NetworkMessage handle(NetworkMessage message) {
        if (message == null) {
            return new NetworkMessage(NetworkMessage.ERROR, "Message không được để trống.");
        }
        return switch (message.getType()) {
            case NetworkMessage.LOGIN -> handleLogin(message);
            case NetworkMessage.REGISTER -> handleRegister(message);
            default -> new NetworkMessage(
                    NetworkMessage.ERROR, "Loại thông điệp tài khoản không hỗ trợ: " + message.getType());
        };
    }

    private NetworkMessage handleLogin(NetworkMessage message) {
        if (!(message.getPayload() instanceof LoginDTO loginDTO)) {
            return new NetworkMessage(NetworkMessage.ERROR, "Payload LOGIN không hợp lệ.");
        }
        LoginResultDTO result = accountService.login(loginDTO);
        return new NetworkMessage(NetworkMessage.LOGIN_RESULT, result);
    }

    private NetworkMessage handleRegister(NetworkMessage message) {
        if (!(message.getPayload() instanceof RegisterRequestDTO registerDTO)) {
            return new NetworkMessage(NetworkMessage.ERROR, "Payload REGISTER không hợp lệ.");
        }
        RegisterResultDTO result = accountService.register(registerDTO);
        return new NetworkMessage(NetworkMessage.REGISTER_RESULT, result);
    }
}
