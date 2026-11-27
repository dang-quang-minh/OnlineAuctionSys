package com.auction.app.client.view;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Điều hướng giữa các màn hình FXML và giữ đồng bộ chủ đề giao diện.
 */
public final class ViewNavigator {
    private static final String VIEW_ROOT =
            "/com/auction/app/client/view/";

    private ViewNavigator() {
    }

    public static void open(String fxmlName, Node source) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                ViewNavigator.class.getResource(VIEW_ROOT + fxmlName));
        Object loaded = loader.load();
        Scene scene = loaded instanceof Scene loadedScene
                ? loadedScene
                : new Scene((Parent) loaded);
        ThemeManager.applyCurrentTheme(scene);
        Stage stage = (Stage) source.getScene().getWindow();
        stage.setTitle("Đấu giá nhanh gọn");
        stage.setScene(scene);
    }
}
