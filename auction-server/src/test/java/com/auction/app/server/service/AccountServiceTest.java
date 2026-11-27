package com.auction.app.server.service;

import com.auction.app.common.dto.LoginDTO;
import com.auction.app.common.dto.LoginResultDTO;
import com.auction.app.common.dto.RegisterRequestDTO;
import com.auction.app.common.dto.RegisterResultDTO;
import com.auction.app.common.model.user.User;
import com.auction.app.server.repository.FileStorageRepository;
import java.io.File;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest {

    @Test
    void seededAccountsCanAuthenticate() {
        AccountService service = new AccountService();

        LoginResultDTO adminResult = service.login(new LoginDTO("admin", "admin123"));
        assertTrue(adminResult.success());
        assertEquals("ADMIN", adminResult.role());
        assertNotNull(adminResult.userId());

        LoginResultDTO demoResult = service.login(new LoginDTO("demo", "demo"));
        assertTrue(demoResult.success());
        assertEquals("BIDDER", demoResult.role());

        LoginResultDTO sellerResult = service.login(new LoginDTO("seller", "demo"));
        assertTrue(sellerResult.success());
        assertEquals("SELLER", sellerResult.role());
    }

    @Test
    void loginWithWrongPasswordFails() {
        AccountService service = new AccountService();

        LoginResultDTO result = service.login(new LoginDTO("admin", "wrongpassword"));
        assertFalse(result.success());
        assertEquals("Tên đăng nhập hoặc mật khẩu không chính xác.", result.message());
    }

    @Test
    void loginWithNonExistentUserFails() {
        AccountService service = new AccountService();

        LoginResultDTO result = service.login(new LoginDTO("nonexistent", "pass"));
        assertFalse(result.success());
    }

    @Test
    void loginWithEmptyOrNullFieldsFails() {
        AccountService service = new AccountService();

        assertFalse(service.login(null).success());
        assertFalse(service.login(new LoginDTO("", "pass")).success());
        assertFalse(service.login(new LoginDTO("admin", "")).success());
    }

    @Test
    void registerNewBidderSuccessfully() {
        AccountService service = new AccountService();

        RegisterRequestDTO request = new RegisterRequestDTO(
                "john_bidder", "john@example.com", "pass123", "BIDDER");
        RegisterResultDTO result = service.register(request);

        assertTrue(result.success());
        assertEquals("Đăng ký thành công.", result.message());

        LoginResultDTO loginResult = service.login(new LoginDTO("john_bidder", "pass123"));
        assertTrue(loginResult.success());
        assertEquals("BIDDER", loginResult.role());

        Optional<User> userOpt = service.findByUsername("john_bidder");
        assertTrue(userOpt.isPresent());
        assertEquals("john@example.com", userOpt.get().getEmail());
    }

    @Test
    void registerNewSellerSuccessfully() {
        AccountService service = new AccountService();

        RegisterRequestDTO request = new RegisterRequestDTO(
                "mary_seller", "mary@example.com", "pass123", "SELLER");
        RegisterResultDTO result = service.register(request);

        assertTrue(result.success());

        LoginResultDTO loginResult = service.login(new LoginDTO("mary_seller", "pass123"));
        assertTrue(loginResult.success());
        assertEquals("SELLER", loginResult.role());
    }

    @Test
    void registerWithDuplicateUsernameFails() {
        AccountService service = new AccountService();

        // Thử đăng ký với tên demo đã có sẵn
        RegisterRequestDTO request = new RegisterRequestDTO(
                "demo", "demo2@example.com", "newpass", "BIDDER");
        RegisterResultDTO result = service.register(request);

        assertFalse(result.success());
        assertEquals("Tên đăng nhập đã tồn tại.", result.message());

        // Kiểm tra không phân biệt hoa thường
        RegisterRequestDTO requestUpper = new RegisterRequestDTO(
                "DEMO", "demo3@example.com", "newpass", "BIDDER");
        assertFalse(service.register(requestUpper).success());
    }

    @Test
    void registerValidationErrors() {
        AccountService service = new AccountService();

        // Null request
        assertFalse(service.register(null).success());

        // Username quá ngắn
        assertFalse(service.register(new RegisterRequestDTO(
                "ab", "ab@test.com", "pass123", "BIDDER")).success());

        // Username chứa ký tự đặc biệt hoặc khoảng trắng
        assertFalse(service.register(new RegisterRequestDTO(
                "user name", "u@test.com", "pass123", "BIDDER")).success());
        assertFalse(service.register(new RegisterRequestDTO(
                "user#name", "u@test.com", "pass123", "BIDDER")).success());

        // Password quá ngắn (< 6 ký tự)
        assertFalse(service.register(new RegisterRequestDTO(
                "validuser", "val@test.com", "12345", "BIDDER")).success());

        // Password chứa khoảng trắng
        assertFalse(service.register(new RegisterRequestDTO(
                "validuser", "val@test.com", "pass 123", "BIDDER")).success());

        // Email không hợp lệ hoặc chứa ký tự đặc biệt không đúng chuẩn (như #)
        assertFalse(service.register(new RegisterRequestDTO(
                "validuser", "no-at-sign", "pass123", "BIDDER")).success());
        assertFalse(service.register(new RegisterRequestDTO(
                "validuser", "mr.dang#@gmail.com", "pass123", "BIDDER")).success());
        assertFalse(service.register(new RegisterRequestDTO(
                "validuser", "test@domain", "pass123", "BIDDER")).success());

        // Role không hợp lệ
        RegisterResultDTO invalidRoleResult = service.register(new RegisterRequestDTO(
                "validuser", "val@test.com", "pass123", "HACKER"));
        assertFalse(invalidRoleResult.success());
        assertEquals("Vai trò không hợp lệ. Chỉ chấp nhận BIDDER hoặc SELLER.",
                invalidRoleResult.message());
    }

    @Test
    void persistenceSavesAndRestoresUsers(@TempDir File tempDir) {
        String storagePath = new File(tempDir, "accounts.dat").getAbsolutePath();
        FileStorageRepository repository = new FileStorageRepository();

        AccountService service1 = new AccountService(repository, storagePath);
        RegisterResultDTO reg = service1.register(new RegisterRequestDTO(
                "persisted_user", "persist@test.com", "secret", "BIDDER"));
        assertTrue(reg.success());

        // Khởi tạo một AccountService mới đọc từ cùng đường dẫn file
        AccountService service2 = new AccountService(repository, storagePath);
        LoginResultDTO loginResult = service2.login(new LoginDTO("persisted_user", "secret"));

        assertTrue(loginResult.success());
        assertEquals("BIDDER", loginResult.role());
    }

    @Test
    void lockedUserCannotLoginUntilUnlocked() {
        AccountService service = new AccountService();

        assertTrue(service.lockUser("demo"));
        LoginResultDTO failedLogin = service.login(new LoginDTO("demo", "demo"));
        assertFalse(failedLogin.success());
        assertEquals("Tài khoản của bạn đã bị khóa bởi quản trị viên.", failedLogin.message());

        assertTrue(service.unlockUser("demo"));
        LoginResultDTO successLogin = service.login(new LoginDTO("demo", "demo"));
        assertTrue(successLogin.success());

        assertFalse(service.lockUser("nonexistent"));
        assertFalse(service.lockUser(null));
        assertFalse(service.unlockUser(null));
    }
}
