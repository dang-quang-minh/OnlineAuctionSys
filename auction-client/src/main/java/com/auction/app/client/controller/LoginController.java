package com.auction.app.client.controller;

import com.auction.app.client.network.AuctionGateway;
import com.auction.app.common.dto.LoginResultDTO;
import com.auction.app.client.view.ViewNavigator;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class LoginController {
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
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = showPasswordCheckBox.isSelected()
                ? visiblePasswordField.getText() : passwordField.getText();
        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("Vui lòng nhập tên đăng nhập và mật khẩu.");
            return;
        }
        Node source = (Node) event.getSource();
        errorLabel.setText("Đang kết nối máy chủ...");
        AuctionGateway.getInstance().login(username, password)
                .whenComplete((result, error) ->
                        javafx.application.Platform.runLater(() -> {
                            if (error != null) {
                                errorLabel.setText("Không thể kết nối máy chủ: "
                                        + errorMessage(error));
                            } else {
                                handleLoginResult(result, source);
                            }
                        }));
    }

    private void handleLoginResult(LoginResultDTO result, Node source) {
        javafx.application.Platform.runLater(() -> {
            if (!result.success()) {
                errorLabel.setText(result.message());
                return;
            }
            errorLabel.setText("");
            try {
                String targetView = resolveViewByRole(result.role());
                ViewNavigator.open(targetView, source);
            } catch (IOException exception) {
                errorLabel.setText("Không thể mở màn hình: " + exception.getMessage());
            }
        });
    }

    private String resolveViewByRole(String role) {
        if ("SELLER".equalsIgnoreCase(role)) {
            return "main-page.fxml";
        }
        if ("ADMIN".equalsIgnoreCase(role)) {
            return "main-page.fxml";
        }
        return "main-page.fxml";
    }

    private static String errorMessage(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause() : error;
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
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
        ViewNavigator.open("signup-view.fxml", (Node) event.getSource());
    }

    @javafx.fxml.FXML
    private void handleToggleTheme(ActionEvent event) {
        com.auction.app.client.view.ThemeManager.toggleTheme(((Node) event.getSource()).getScene());
    }
}
