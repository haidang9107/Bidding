package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.session.Session;
import org.example.client.util.SceneRouter;
import org.example.model.user.Role;
import org.example.model.user.User;
import org.example.payload.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.payload.dto.LoginRequest;
import org.example.util.JsonConverter;

/**
 * Controller cho màn hình đăng nhập.
 * MVC: controller chỉ xử lý input + gọi network, không chứa business logic.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Button registerBtn;
    @FXML private Label statusLabel;

    private final SocketClient client = SocketClient.getInstance();
    private ServerListener listener;

    @FXML
    public void initialize() {
        // Đăng ký listener nhận response từ server
        listener = this::handleResponse;
        client.addListener(listener);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đủ thông tin");
            return;
        }

        loginBtn.setDisable(true);
        statusLabel.setText("Đang đăng nhập...");

        LoginRequest lr = new LoginRequest(username, password);
        Request req = new Request(MessageType.LOGIN, JsonConverter.toJson(lr));
        client.send(req);
    }

    @FXML
    private void handleRegister() {
        // Gỡ listener trước khi chuyển scene
        client.removeListener(listener);
        SceneRouter.go("/view/Register.fxml", "Đăng ký");
    }

    private void handleResponse(Response resp) {
        if (resp.getType() != MessageType.LOGIN) {
            return;  // không phải response của mình
        }
        Platform.runLater(() -> {
            loginBtn.setDisable(false);
            if (resp.isSuccess()) {
                User user = JsonConverter.fromJson(resp.getData(), User.class);
                Session.getInstance().setCurrentUser(user);
                statusLabel.setText("");
                // Gỡ listener cũ trước khi chuyển scene
                client.removeListener(listener);
                // Chuyển scene theo vai trò
                if (user.getRole() == Role.SELLER) {
                    SceneRouter.go("/view/SellerDashboard.fxml",
                            "Seller Dashboard - " + user.getFullName());
                } else {
                    SceneRouter.go("/view/AuctionList.fxml",
                            "Danh sách phiên đấu giá");
                }
            } else {
                statusLabel.setText("Lỗi: " + resp.getMessage());
            }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
