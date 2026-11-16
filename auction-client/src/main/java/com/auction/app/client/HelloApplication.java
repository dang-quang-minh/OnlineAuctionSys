package com.auction.app.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("/com/auction/app/client/view/login-view.fxml"));
        Scene scene = (Scene) fxmlLoader.load();
        stage.setTitle("Online Auction System");
        stage.setScene(scene);
        stage.show();
    }
}
