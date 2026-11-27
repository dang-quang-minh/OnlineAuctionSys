package com.auction.app.client;

import com.auction.app.client.view.ThemeManager;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("/com/auction/app/client/view/login-view.fxml"));
        Object loaded = fxmlLoader.load();
        Scene scene = loaded instanceof Scene s ? s : new Scene((Parent) loaded);
        ThemeManager.applyCurrentTheme(scene);
        stage.setTitle("Đấu giá nhanh gọn");
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        stage.show();
    }
}
