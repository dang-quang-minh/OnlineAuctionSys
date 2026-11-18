package com.auction.app.client.demoData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoAccountServiceTest {
    @Test
    void authenticatesOnlyDemoAccount() {
        assertTrue(DemoAccountService.authenticate("demo", "demo"));
        assertFalse(DemoAccountService.authenticate("demo", "wrong"));
        assertFalse(DemoAccountService.authenticate("", "demo"));
    }

    @Test
    void validatesDemoRegistrationForm() {
        assertNull(DemoAccountService.validateRegistration("new-user", "1234", "1234"));
        assertEquals(
                "Vui lòng nhập đầy đủ thông tin.",
                DemoAccountService.validateRegistration("", "1234", "1234"));
        assertEquals(
                "Mật khẩu xác nhận không khớp.",
                DemoAccountService.validateRegistration("new-user", "1234", "4321"));
    }
}
