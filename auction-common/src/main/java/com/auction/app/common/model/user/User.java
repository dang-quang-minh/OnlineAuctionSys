package com.auction.app.common.model.user;

import java.io.Serializable;
import java.util.Objects;

/** Thông tin chung của người dùng trong hệ thống đấu giá. */
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    // Các thông tin về người dùng là immutable
    private final String id;
    private final String username;
    private final String password;
    private final String email;
    private Boolean active = Boolean.TRUE;

    protected User(String id, String username, String password, String email) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");
        this.email = Objects.requireNonNull(email, "email");
        this.active = Boolean.TRUE;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active == null || active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.active == null) {
            this.active = Boolean.TRUE;
        }
    }

    /**
     * Mã hóa mật khẩu bằng thuật toán SHA-256 một chiều bảo mật.
     */
    public static String hashPassword(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Thuật toán SHA-256 không khả dụng", e);
        }
    }

    /**
     * Xác thực mật khẩu đóng gói an toàn (hỗ trợ cả mật khẩu đã băm và kiểm tra plain text).
     */
    public boolean authenticate(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        String hashed = hashPassword(rawPassword);
        return this.password.equals(hashed) || this.password.equals(rawPassword);
    }

    public boolean canBid() {
        return false;
    }

    public boolean canSell() {
        return false;
    }

    public boolean isAdmin() {
        return false;
    }

    /** Trả về vai trò cụ thể của loại người dùng. */
    public abstract String getRole();
}
