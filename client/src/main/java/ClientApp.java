import javafx.application.Application;
import javafx.stage.Stage;
import org.example.client.network.SocketClient;
import org.example.client.util.SceneRouter;

/**
 * Entry point của Client.
 *  - Kết nối tới server qua socket (Singleton SocketClient)
 *  - Khởi tạo SceneRouter (quản lý chuyển scene)
 *  - Mở màn hình Login đầu tiên.
 */
public class ClientApp extends Application {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    @Override
    public void start(Stage stage) {
        try {
            // 1) Kết nối server
            SocketClient.getInstance().connect(SERVER_HOST, SERVER_PORT);
            System.out.println(">>> Đã kết nối server");

            // 2) Mở Login scene
            SceneRouter.init(stage);
            SceneRouter.go("/view/Login.fxml", "Đăng nhập");
            stage.setOnCloseRequest(e -> {
                SocketClient.getInstance().disconnect();
                System.exit(0);
            });
            stage.show();
        } catch (Exception e) {
            System.err.println(">>> Không kết nối được server: " + e.getMessage());
            e.printStackTrace();
            // Hiện dialog báo lỗi rồi exit
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
