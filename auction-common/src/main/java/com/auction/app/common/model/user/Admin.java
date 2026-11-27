package com.auction.app.common.model.user;

import com.auction.app.common.model.Auction;
import java.util.Objects;

/** Người dùng quản trị hệ thống. */
public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin(String id, String username, String password, String email) {
        super(id, username, password, email);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public boolean isAdmin() {
        return true;
    }

    /**
     * Khóa tài khoản người dùng vi phạm.
     */
    public void lockUser(User targetUser) {
        Objects.requireNonNull(targetUser, "targetUser không được null");
        targetUser.setActive(false);
    }

    /**
     * Mở khóa tài khoản người dùng.
     */
    public void unlockUser(User targetUser) {
        Objects.requireNonNull(targetUser, "targetUser không được null");
        targetUser.setActive(true);
    }

    /**
     * Kiểm tra quyền hủy phiên đấu giá bất thường của quản trị viên.
     */
    public boolean canCancelAuction(Auction auction) {
        return auction != null;
    }
}
