package com.auction.app.client.controller;

import com.auction.app.common.dto.LoginDTO;
import com.auction.app.common.dto.LoginResultDTO;
import com.auction.app.common.dto.NetworkMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    private static final String DEMO_USERNAME = "demo";
    private static final String DEMO_PASSWORD = "demo";

    @javafx.fxml.FXML
    private TextField usernameField;
    @javafx.fxml.FXML
    private PasswordField passwordField;
    @javafx.fxml.FXML
    private TextField visiblePasswordField;
    @javafx.fxml.FXML
    private CheckBox showPasswordCheckBox;
    @javafx.fxml.FXML
    private Label errorLabel;

    @javafx.fxml.FXML
    private void handleLogin(ActionEvent event) throws IOException {
        String username = usernameField.getText().trim();
        String password = showPasswordCheckBox.isSelected()
                ? visiblePasswordField.getText() : passwordField.getText();
        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("Vui lòng nhập tên đăng nhập và mật khẩu.");
            return;
        }
        LoginDTO loginRequest = new LoginDTO(username, password);
        if (!DEMO_USERNAME.equals(loginRequest.username())
                || !DEMO_PASSWORD.equals(loginRequest.password())) {
            errorLabel.setText("Demo: dùng tài khoản demo / demo.");
            return;
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/auction/app/client/view/main-page.fxml"));
        Scene scene = new Scene(loader.load());
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).setScene(scene);
    }

    /**
     * Handles the shared LOGIN_RESULT contract when a network listener is wired.
     */
    public void handleNetworkMessage(NetworkMessage message) {
        if (message == null || !NetworkMessage.LOGIN_RESULT.equals(message.getType())) {
            return;
        }
        if (!(message.getPayload() instanceof LoginResultDTO result)) {
            errorLabel.setText("Phản hồi đăng nhập từ máy chủ không hợp lệ.");
            return;
        }
        errorLabel.setText(result.success() ? "" : result.message());
    }

    @javafx.fxml.FXML
    private void togglePasswordVisibility() {
        boolean visible = showPasswordCheckBox.isSelected();
        if (visible) {
            visiblePasswordField.setText(passwordField.getText());
        } else {
            passwordField.setText(visiblePasswordField.getText());
        }
        passwordField.setVisible(!visible);
        passwordField.setManaged(!visible);
        visiblePasswordField.setVisible(visible);
        visiblePasswordField.setManaged(visible);
    }

    @javafx.fxml.FXML
    private void openSignup(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/auction/app/client/view/signup-view.fxml"));
        Scene scene = (Scene) loader.load();
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).setScene(scene);
    }
}
