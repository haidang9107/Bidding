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

    private SceneRouter() {
    }

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /** Chuyển scene cơ bản. */
    public static void go(String fxmlPath, String title) {
        go(fxmlPath, title, null);
    }

    /**
     * Chuyển scene và truyền dữ liệu vào controller.
     * controllerInit được gọi sau khi FXML load xong, trước khi show.
     */
    @SuppressWarnings("unchecked")
    public static <T> void go(String fxmlPath, String title, Consumer<T> controllerInit) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneRouter.class.getResource(fxmlPath));
            Parent root = loader.load();

            if (controllerInit != null) {
                T controller = loader.getController();
                controllerInit.accept(controller);
            }

            Scene scene = new Scene(root);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException("Không load được FXML: " + fxmlPath, e);
        }
    }
}
