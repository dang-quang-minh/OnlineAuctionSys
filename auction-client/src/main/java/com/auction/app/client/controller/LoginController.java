package com.auction.app.client.controller;

import com.auction.app.client.demoData.DemoAccountService;
import com.auction.app.client.view.ViewNavigator;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

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
    private void handleLogin(ActionEvent event) throws IOException {
        String username = usernameField.getText().trim();
        String password = showPasswordCheckBox.isSelected()
                ? visiblePasswordField.getText() : passwordField.getText();
        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("Vui lòng nhập tên đăng nhập và mật khẩu.");
            return;
        }
        if (!DemoAccountService.authenticate(username, password)) {
            errorLabel.setText("Demo: dùng tài khoản demo / demo.");
            return;
        }
        errorLabel.setText("");
        ViewNavigator.open("main-page.fxml", (Node) event.getSource());
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
}
