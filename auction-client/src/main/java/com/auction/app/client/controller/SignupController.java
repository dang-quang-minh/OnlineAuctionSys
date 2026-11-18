package com.auction.app.client.controller;

import com.auction.app.client.demoData.DemoAccountService;
import com.auction.app.client.view.ViewNavigator;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class SignupController {
    @javafx.fxml.FXML
    private TextField usernameField;
    @javafx.fxml.FXML
    private PasswordField passwordField;
    @javafx.fxml.FXML
    private PasswordField confirmPasswordField;
    @javafx.fxml.FXML
    private Label errorLabel;

    @javafx.fxml.FXML
    private void handleSignup() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String validationError = DemoAccountService.validateRegistration(
                username,
                password,
                confirmPasswordField.getText());
        if (validationError != null) {
            errorLabel.setText(validationError);
        } else {
            errorLabel.setText("Đăng ký demo thành công. Hãy quay lại đăng nhập.");
        }
    }

    @javafx.fxml.FXML
    private void backToLogin(ActionEvent event) throws IOException {
        ViewNavigator.open("login-view.fxml", (Node) event.getSource());
    }
}
