package org.example.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.session.Session;
import org.example.client.util.SceneRouter;
import org.example.dto.request.LoginRequest;
import org.example.model.enums.MessageType;
import org.example.model.enums.UserRole;
import org.example.model.user.Admin;
import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

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
        listener = this::handleResponse;
        client.addListener(listener);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đủ thông tin!");
            return;
        }

        loginBtn.setDisable(true);
        statusLabel.setText("Đang kết nối tới hệ thống...");

        // Sau khi bị kick/ban, server đã đóng socket (connected=false). Đảm bảo
        // kết nối lại trước khi gửi LOGIN, nếu không send() sẽ âm thầm bỏ qua.
        if (!client.isConnected()) {
            try {
                client.reconnect();
            } catch (Exception e) {
                loginBtn.setDisable(false);
                statusLabel.setText("Không kết nối được máy chủ. Thử lại.");
                return;
            }
        }

        LoginRequest lr = new LoginRequest(username, password);
        Request req = new Request(MessageType.LOGIN, lr);
        client.send(req);
    }

    @FXML
    private void handleRegister() {
        cleanup();
        SceneRouter.go("/view/Register.fxml", "Đăng ký tài khoản");
    }

    private void handleResponse(Response resp) {
        MessageType t = resp.getType();
        // Server may return type=SUCCESS for the login + a UserResponse in data,
        // or it may set type=LOGIN. Accept both.
        if (t != MessageType.LOGIN && t != MessageType.SUCCESS && t != MessageType.ERROR) return;

        Platform.runLater(() -> {
            loginBtn.setDisable(false);

            if (resp.isSuccess() && resp.getData() != null) {
                String userJson = JsonConverter.toJson(resp.getData());
                // Decide role first by probing.
                User probe = JsonConverter.fromJson(userJson, Admin.class);
                User user;
                if (probe != null && probe.getRole() == UserRole.ADMIN) {
                    user = probe;
                } else {
                    user = JsonConverter.fromJson(userJson, Member.class);
                }
                if (user == null) {
                    statusLabel.setText("Phản hồi không hợp lệ từ server.");
                    return;
                }

                Session.getInstance().setCurrentUser(user);
                statusLabel.setText("Đăng nhập thành công!");
                cleanup();
                redirectByRole(user);
            } else if (resp.getType() == MessageType.ERROR
                       || (resp.getType() == MessageType.LOGIN && !resp.isSuccess())) {
                statusLabel.setText("Lỗi: " + resp.getMessage());
            }
        });
    }

    private void redirectByRole(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            SceneRouter.go("/view/AdminDashboard.fxml", "Quản trị viên");
        } else if (user.getRole() == UserRole.MEMBER) {
            SceneRouter.go("/view/UserDashboard.fxml", "Trang người dùng");
        } else {
            SceneRouter.go("/view/AuctionList.fxml", "Sàn đấu giá trực tuyến");
        }
    }

    private void cleanup() {
        client.removeListener(listener);
    }
}
