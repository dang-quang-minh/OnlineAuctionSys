package com.auction.app.client.demoData;

/**
 * Presentation-only demo account rules for the Week 8 JavaFX flow.
 *
 * <p>This service does not authenticate against or register with a server.
 * The real account flow belongs to the networking implementation.</p>
 */
public final class DemoAccountService {
    private static final String DEMO_USERNAME = "demo";
    private static final String DEMO_PASSWORD = "demo";

    private DemoAccountService() {
    }

    public static boolean authenticate(String username, String password) {
        return DEMO_USERNAME.equals(username) && DEMO_PASSWORD.equals(password);
    }

    public static String validateRegistration(
            String username,
            String password,
            String confirmation) {
        if (username.isBlank() || password.isBlank() || confirmation.isBlank()) {
            return "Vui lòng nhập đầy đủ thông tin.";
        }
        if (!password.equals(confirmation)) {
            return "Mật khẩu xác nhận không khớp.";
        }
        return null;
    }
}
