package com.auction.app.client.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Loads client FXML screens and switches the current JavaFX stage.
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
        Stage stage = (Stage) source.getScene().getWindow();
        stage.setScene(scene);
    }
}
