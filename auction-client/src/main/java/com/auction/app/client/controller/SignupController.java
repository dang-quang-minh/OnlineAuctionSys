package com.auction.app.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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
        if (username.isBlank() || password.isBlank() || confirmPasswordField.getText().isBlank()) {
            errorLabel.setText("Vui lòng nhập đầy đủ thông tin.");
        } else if (!password.equals(confirmPasswordField.getText())) {
            errorLabel.setText("Mật khẩu xác nhận không khớp.");
        } else {
            errorLabel.setText("Đăng ký demo thành công. Hãy quay lại đăng nhập.");
        }
    }

    @javafx.fxml.FXML
    private void backToLogin(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/auction/app/client/view/login-view.fxml"));
        Scene scene = (Scene) loader.load();
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).setScene(scene);
    }
}
