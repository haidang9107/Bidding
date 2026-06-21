package org.example.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.example.client.network.SocketClient;
import org.example.client.session.Session;
import org.example.client.util.SceneRouter;
import org.example.model.enums.MessageType;
import org.example.util.Config;

/**
 * Entry point của Client.
 *  - Kết nối tới server qua socket (Singleton SocketClient)
 *  - Khởi tạo SceneRouter (quản lý chuyển scene)
 *  - Mở màn hình Login đầu tiên.
 *  - Cửa sổ bắt đầu ở trạng thái maximized; SceneRouter giữ kích thước qua
 *    mọi lần đổi scene.
 *  - Khi user nhấn nút X, ngắt socket trên background thread + exit ngay
 *    để tránh delay (close socket có thể block đến vài giây khi server
 *    không phản hồi).
 */
public class ClientApp extends Application {

    private static final String SERVER_HOST = Config.get("SERVER_HOST");
    private static final int SERVER_PORT = Config.getInt("SERVER_PORT");

    @Override
    public void start(Stage stage) {
        try {
            // 1) Kết nối server
            SocketClient.getInstance().connect(SERVER_HOST, SERVER_PORT);
            System.out.println(">>> Đã kết nối server");

            // 2) Cấu hình stage: minimum size + maximized ngay từ đầu để
            //    người dùng không cần phóng to bằng tay.
            javafx.geometry.Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            stage.setMinWidth(1000);
            stage.setMinHeight(700);
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            stage.setMaximized(true);

            // 3) Open Login
            SceneRouter.init(stage);
            SceneRouter.go("/view/Login.fxml", "Đăng nhập");

            // 4) Global Logout & Kick Listener: Tự động về Login nếu bị Logout hoặc Ban từ server
            SocketClient.getInstance().addListener(resp -> {
                MessageType type = resp.getType();
                if (type == MessageType.LOGOUT ||
                    (type == MessageType.SUCCESS && "Logout successful".equals(resp.getMessage())) ||
                    (type == MessageType.ERROR && "ACCOUNT_BANNED".equals(resp.getData()))) {

                    Platform.runLater(() -> {
                        // Nếu đang ở màn hình Login rồi thì không cần redirect nữa
                        if ("/view/Login.fxml".equals(SceneRouter.getCurrentFxml())) {
                            return;
                        }

                        Session.getInstance().logout();
                        SceneRouter.go("/view/Login.fxml", "Đăng nhập");

                        if (type == MessageType.ERROR && "ACCOUNT_BANNED".equals(resp.getData())) {
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                    javafx.scene.control.Alert.AlertType.WARNING);
                            alert.setTitle("Tài khoản bị khoá");
                            alert.setHeaderText(null);
                            alert.setContentText(resp.getMessage());
                            alert.show();
                        }
                    });
                }
            });

            // 5) Xử lý đóng app: chạy disconnect trên background thread để
            //    UI không bị block (socket.close() có thể chờ đến vài giây
            //    nếu server không phản hồi). Halt() đảm bảo JVM dừng ngay,
            //    không chờ daemon threads.
            stage.setOnCloseRequest(e -> {
                e.consume();              // tự xử lý thay vì để JavaFX đóng từ từ
                Thread t = new Thread(() -> {
                    try { SocketClient.getInstance().disconnect(); }
                    catch (Exception ignored) {}
                    Platform.exit();
                    // Bảo hiểm: nếu có thread non-daemon còn sống thì halt.
                    Runtime.getRuntime().halt(0);
                }, "client-shutdown");
                t.setDaemon(true);
                t.start();
            });
            stage.show();
        } catch (Exception e) {
            System.err.println(">>> Không kết nối được server: " + e.getMessage());
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Lỗi kết nối");
            alert.setHeaderText(null);
            alert.setContentText(
                    "Không kết nối được server " + SERVER_HOST + ":" + SERVER_PORT
                            + "\nVui lòng khởi động server trước.");
            alert.showAndWait();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
