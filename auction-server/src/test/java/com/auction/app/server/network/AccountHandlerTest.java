package com.auction.app.server.network;

import com.auction.app.common.dto.LoginDTO;
import com.auction.app.common.dto.LoginResultDTO;
import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.dto.RegisterRequestDTO;
import com.auction.app.common.dto.RegisterResultDTO;
import com.auction.app.server.service.AccountService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountHandlerTest {

    @Test
    void handleLoginReturnsLoginResult() {
        AccountService service = new AccountService();
        AccountHandler handler = new AccountHandler(service);

        NetworkMessage request = new NetworkMessage(
                NetworkMessage.LOGIN, new LoginDTO("demo", "demo"));
        NetworkMessage response = handler.handle(request);

        assertNotNull(response);
        assertEquals(NetworkMessage.LOGIN_RESULT, response.getType());
        assertTrue(response.getPayload() instanceof LoginResultDTO);
        LoginResultDTO result = (LoginResultDTO) response.getPayload();
        assertTrue(result.success());
    }

    @Test
    void handleRegisterReturnsRegisterResult() {
        AccountService service = new AccountService();
        AccountHandler handler = new AccountHandler(service);

        NetworkMessage request = new NetworkMessage(
                NetworkMessage.REGISTER,
                new RegisterRequestDTO("handler_user", "h@test.com", "pass123", "BIDDER"));
        NetworkMessage response = handler.handle(request);

        assertNotNull(response);
        assertEquals(NetworkMessage.REGISTER_RESULT, response.getType());
        assertTrue(response.getPayload() instanceof RegisterResultDTO);
        RegisterResultDTO result = (RegisterResultDTO) response.getPayload();
        assertTrue(result.success());
    }

    @Test
    void handleInvalidPayloadOrTypeReturnsError() {
        AccountService service = new AccountService();
        AccountHandler handler = new AccountHandler(service);

        NetworkMessage badLogin = new NetworkMessage(NetworkMessage.LOGIN, "not-a-dto");
        assertEquals(NetworkMessage.ERROR, handler.handle(badLogin).getType());

        NetworkMessage badRegister = new NetworkMessage(NetworkMessage.REGISTER, "not-a-dto");
        assertEquals(NetworkMessage.ERROR, handler.handle(badRegister).getType());

        NetworkMessage unknown = new NetworkMessage("UNKNOWN_TYPE", "payload");
        assertEquals(NetworkMessage.ERROR, handler.handle(unknown).getType());

        assertEquals(NetworkMessage.ERROR, handler.handle(null).getType());
    }
}
