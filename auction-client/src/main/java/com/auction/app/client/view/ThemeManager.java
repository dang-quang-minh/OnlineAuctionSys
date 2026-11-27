package com.auction.app.client.view;

import javafx.scene.Scene;

/**
 * Quản lý chế độ giao diện Sáng / Tối (Light / Dark Theme) cho ứng dụng.
 */
public final class ThemeManager {

    private static boolean darkMode = false;

    private ThemeManager() {
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void setDarkMode(boolean dark) {
        darkMode = dark;
    }

    /**
     * Chuyển đổi qua lại giữa chế độ Sáng và Tối.
     */
    public static void toggleTheme(Scene scene) {
        darkMode = !darkMode;
        applyCurrentTheme(scene);
    }

    /**
     * Áp dụng chủ đề hiện tại lên scene đã cung cấp.
     */
    public static void applyCurrentTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        scene.getRoot().getStyleClass().remove("light-theme");
        scene.getRoot().getStyleClass().remove("dark-theme");
        if (darkMode) {
            scene.getRoot().getStyleClass().add("dark-theme");
        } else {
            scene.getRoot().getStyleClass().add("light-theme");
        }
    }
}
