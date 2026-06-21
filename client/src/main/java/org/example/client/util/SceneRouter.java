package org.example.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Quản lý chuyển scene cho client. Đơn giản hóa việc load FXML.
 *
 * Cách dùng:
 *   SceneRouter.go("/view/Login.fxml", "Đăng nhập");
 *   SceneRouter.go("/view/AuctionDetail.fxml", "Phiên #1",
 *                  (AuctionDetailController c) -> c.setAuctionId(1L));
 */
public final class SceneRouter {

    private static Stage primaryStage;
    private static String currentFxml;

    private SceneRouter() {
    }

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static String getCurrentFxml() {
        return currentFxml;
    }

    /** Chuyển scene cơ bản. */
    public static void go(String fxmlPath, String title) {
        go(fxmlPath, title, null);
    }

    /**
     * Chuyển scene và truyền dữ liệu vào controller.
     * controllerInit được gọi sau khi FXML load xong, trước khi show.
     *
     * <p>Quan trọng: thay vì tạo Scene mới mỗi lần (làm window co lại về
     * kích thước root mặc định và mất trạng thái maximized), ta chỉ
     * setRoot() trên Scene đã có. Cách này giữ nguyên vị trí, kích thước,
     * và trạng thái phóng to/thu nhỏ qua mọi lần điều hướng.</p>
     */
    @SuppressWarnings("unchecked")
    public static <T> void go(String fxmlPath, String title, Consumer<T> controllerInit) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneRouter.class.getResource(fxmlPath));
            Parent root = loader.load();
            currentFxml = fxmlPath;

            if (controllerInit != null) {
                T controller = loader.getController();
                controllerInit.accept(controller);
            }

            // Lưu lại trạng thái maximized để re-apply sau (một số platform
            // reset maximized khi đổi Scene).
            boolean wasMaximized = primaryStage.isMaximized();
            double w = primaryStage.getWidth();
            double h = primaryStage.getHeight();

            if (primaryStage.getScene() == null) {
                primaryStage.setScene(new Scene(root, w > 0 ? w : 1280, h > 0 ? h : 800));
            } else {
                // Tái sử dụng Scene để không mất kích thước / maximized.
                primaryStage.getScene().setRoot(root);
            }
            primaryStage.setTitle(title);

            // Re-apply maximized state if it got cleared.
            if (wasMaximized && !primaryStage.isMaximized()) {
                primaryStage.setMaximized(true);
            }
        } catch (Exception e) {
            // Một lỗi load FXML (thiếu handler, controller cũ, file thiếu...)
            // nếu chỉ ném RuntimeException sẽ bị JavaFX nuốt trong event
            // handler — nút bấm trông như "chết" mà không ai biết vì sao.
            // Hiện Alert để người dùng/dev thấy ngay nguyên nhân.
            e.printStackTrace();
            try {
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR);
                a.setTitle("Lỗi giao diện");
                a.setHeaderText("Không mở được màn hình: " + fxmlPath);
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                a.setContentText(String.valueOf(cause));
                a.showAndWait();
            } catch (Exception ignored) {}
        }
    }
}
