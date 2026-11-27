package com.auction.app.server.service;

import com.auction.app.common.dto.LoginDTO;
import com.auction.app.common.dto.LoginResultDTO;
import com.auction.app.common.dto.RegisterRequestDTO;
import com.auction.app.common.dto.RegisterResultDTO;
import com.auction.app.common.model.user.Admin;
import com.auction.app.common.model.user.Bidder;
import com.auction.app.common.model.user.Seller;
import com.auction.app.common.model.user.User;
import com.auction.app.server.repository.AuctionDataSnapshot;
import com.auction.app.server.repository.StorageRepository;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Dịch vụ quản lý tài khoản, xác thực và phân quyền người dùng.
 */
public class AccountService {

    private static final System.Logger LOGGER = System.getLogger(AccountService.class.getName());

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    private final ConcurrentMap<String, User> usersById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final StorageRepository storageRepository;
    private final String storagePath;

    public AccountService() {
        this(null, null);
    }

    public AccountService(StorageRepository storageRepository, String storagePath) {
        this.storageRepository = storageRepository;
        this.storagePath = storagePath;
        seedDefaultAccounts();
        loadFromStorageIfAvailable();
    }

    private void seedDefaultAccounts() {
        // Tài khoản Admin mặc định với mật khẩu đã mã hóa SHA-256
        registerUserInternal(new Admin("admin-001", "admin", User.hashPassword("admin123"), "admin@auction.com"));
        // Tài khoản Demo Bidder cho tương thích kiểm thử
        registerUserInternal(new Bidder("demo-bidder", "demo", User.hashPassword("demo"), "demo@auction.com"));
        // Tài khoản Demo Seller
        registerUserInternal(new Seller("seller-demo", "seller", User.hashPassword("demo"), "seller@example.com"));
    }

    private void registerUserInternal(User user) {
        usersById.put(user.getId(), user);
        usersByUsername.put(user.getUsername().toLowerCase(Locale.ROOT), user);
    }

    /**
     * Xử lý đăng ký tài khoản mới.
     */
    public RegisterResultDTO register(RegisterRequestDTO request) {
        if (request == null) {
            return new RegisterResultDTO(false, "Yêu cầu đăng ký không được để trống.");
        }

        String username = request.username() != null ? request.username().trim() : "";
        String password = request.password() != null ? request.password() : "";
        String email = request.email() != null ? request.email().trim() : "";
        String roleStr = request.role() != null ? request.role().trim().toUpperCase(Locale.ROOT) : "";

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return new RegisterResultDTO(
                    false, "Tên đăng nhập phải từ 3-20 ký tự, chỉ gồm chữ cái, số và dấu gạch dưới (_).");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return new RegisterResultDTO(
                    false, "Địa chỉ email không đúng định dạng (ví dụ: user@example.com).");
        }
        if (password.length() < 6 || password.contains(" ")) {
            return new RegisterResultDTO(
                    false, "Mật khẩu phải có ít nhất 6 ký tự và không chứa khoảng trắng.");
        }

        if (!"BIDDER".equals(roleStr) && !"SELLER".equals(roleStr)) {
            return new RegisterResultDTO(false, "Vai trò không hợp lệ. Chỉ chấp nhận BIDDER hoặc SELLER.");
        }

        String lookupKey = username.toLowerCase(Locale.ROOT);
        if (usersByUsername.containsKey(lookupKey)) {
            return new RegisterResultDTO(false, "Tên đăng nhập đã tồn tại.");
        }

        String userId = UUID.randomUUID().toString();
        String hashedPassword = User.hashPassword(password);
        User newUser;
        if ("SELLER".equals(roleStr)) {
            newUser = new Seller(userId, username, hashedPassword, email);
        } else {
            newUser = new Bidder(userId, username, hashedPassword, email);
        }

        if (usersByUsername.putIfAbsent(lookupKey, newUser) != null) {
            return new RegisterResultDTO(false, "Tên đăng nhập đã tồn tại.");
        }
        usersById.put(userId, newUser);

        persistState();
        return new RegisterResultDTO(true, "Đăng ký thành công.");
    }

    /**
     * Xử lý đăng nhập và trả kết quả kèm thông tin định danh & vai trò.
     */
    public LoginResultDTO login(LoginDTO request) {
        if (request == null) {
            return new LoginResultDTO(false, "Yêu cầu đăng nhập không được để trống.", null, null);
        }

        String username = request.username() != null ? request.username().trim() : "";
        String password = request.password() != null ? request.password() : "";

        if (username.isBlank() || password.isBlank()) {
            return new LoginResultDTO(false, "Tên đăng nhập và mật khẩu không được để trống.", null, null);
        }

        User user = usersByUsername.get(username.toLowerCase(Locale.ROOT));
        if (user == null || !user.authenticate(password)) {
            return new LoginResultDTO(false, "Tên đăng nhập hoặc mật khẩu không chính xác.", null, null);
        }

        if (!user.isActive()) {
            return new LoginResultDTO(false, "Tài khoản của bạn đã bị khóa bởi quản trị viên.", null, null);
        }

        return new LoginResultDTO(true, "Đăng nhập thành công.", user.getId(), user.getRole());
    }

    /**
     * Khóa tài khoản người dùng bởi quản trị viên.
     */
    public boolean lockUser(String username) {
        if (username == null) {
            return false;
        }
        User user = usersByUsername.get(username.toLowerCase(Locale.ROOT));
        if (user != null) {
            user.setActive(false);
            persistState();
            return true;
        }
        return false;
    }

    /**
     * Mở khóa tài khoản người dùng.
     */
    public boolean unlockUser(String username) {
        if (username == null) {
            return false;
        }
        User user = usersByUsername.get(username.toLowerCase(Locale.ROOT));
        if (user != null) {
            user.setActive(true);
            persistState();
            return true;
        }
        return false;
    }

    public Optional<User> findById(String userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersById.get(userId));
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersByUsername.get(username.toLowerCase(Locale.ROOT)));
    }

    public Map<String, User> getAllUsers() {
        return new HashMap<>(usersById);
    }

    public void save() {
        persistState();
    }

    public void load() {
        loadFromStorageIfAvailable();
    }

    private void persistState() {
        if (storageRepository == null || storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            AuctionDataSnapshot snapshot = new AuctionDataSnapshot(usersById, null, null);
            storageRepository.save(storagePath, snapshot);
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Không thể lưu dữ liệu tài khoản xuống file.", e);
        }
    }

    private void loadFromStorageIfAvailable() {
        if (storageRepository == null || storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            AuctionDataSnapshot snapshot = storageRepository.load(storagePath);
            if (snapshot != null && snapshot.getUsers() != null) {
                for (User user : snapshot.getUsers().values()) {
                    if ("admin".equalsIgnoreCase(user.getUsername())
                            || "demo".equalsIgnoreCase(user.getUsername())
                            || "seller".equalsIgnoreCase(user.getUsername())) {
                        user.setActive(true);
                    }
                    registerUserInternal(user);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Không thể tải dữ liệu tài khoản từ file.", e);
        }
    }
}
