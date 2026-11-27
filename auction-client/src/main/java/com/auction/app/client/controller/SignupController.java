package com.auction.app.client.controller;

import com.auction.app.client.network.ClientSession;
import com.auction.app.client.view.ViewNavigator;
import com.auction.app.common.dto.RegisterRequestDTO;
import java.io.IOException;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignupController {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void initialize() {
        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList("BIDDER", "SELLER"));
            roleComboBox.setValue("BIDDER");
        }
    }

    @FXML
    private void handleSignup() {
        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String email = emailField != null && emailField.getText() != null ? emailField.getText().trim() : "";
        String role = roleComboBox != null && roleComboBox.getValue() != null
                ? roleComboBox.getValue() : "BIDDER";
        String password = passwordField.getText() != null ? passwordField.getText() : "";
        String confirmPassword = confirmPasswordField.getText() != null ? confirmPasswordField.getText() : "";

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            showError("Vui lòng nhập đầy đủ tất cả các trường.");
            return;
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            showError("Tên đăng nhập từ 3-20 ký tự, chỉ chứa chữ cái, số và dấu gạch dưới (_).");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Email không đúng định dạng (ví dụ: name@example.com).");
            return;
        }
        if (password.length() < 6 || password.contains(" ")) {
            showError("Mật khẩu phải từ 6 ký tự trở lên và không chứa khoảng trắng.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp.");
            return;
        }

        RegisterRequestDTO request = new RegisterRequestDTO(username, email, password, role);
        errorLabel.setStyle("-fx-text-fill: #555555;");
        errorLabel.setText("Đang gửi thông tin đăng ký...");

        try {
            ClientSession.getInstance().register(
                    request,
                    result -> Platform.runLater(() -> {
                        if (result.success()) {
                            errorLabel.setStyle("-fx-text-fill: #2e7d32;");
                            errorLabel.setText(result.message() + " Hãy chuyển sang đăng nhập.");
                        } else {
                            showError(result.message());
                        }
                    }),
                    error -> Platform.runLater(() -> showError("Lỗi: " + error))
            );
        } catch (IOException e) {
            showError("Không thể kết nối máy chủ: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setStyle("-fx-text-fill: #c62828;");
        errorLabel.setText(message);
    }

    @FXML
    private void backToLogin(ActionEvent event) throws IOException {
        ViewNavigator.open("login-view.fxml", (Node) event.getSource());
    }

    @FXML
    private void handleToggleTheme(ActionEvent event) {
        com.auction.app.client.view.ThemeManager.toggleTheme(((Node) event.getSource()).getScene());
    }
}
