package org.example.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.session.Session;
import org.example.client.util.SceneRouter;
import org.example.model.enums.UserRole;
import org.example.model.user.Admin;
import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.payload.dto.LoginRequest;
import org.example.util.JsonConverter;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Label statusLabel;

    private final SocketClient client = SocketClient.getInstance();
    private ServerListener listener;

    @FXML
    public void initialize() {
        // Tạo listener và đăng ký với SocketClient
        listener = this::handleResponse;
        client.addListener(listener);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đủ thông tin!");
            return;
        }

        loginBtn.setDisable(true);// khóa nút bấm lại chỉ cho gửi yêu cầu đăng nhập một lần
        statusLabel.setText("Đang kết nối tới hệ thống...");

        // Đóng gói LoginRequest -> JSON -> Request
        LoginRequest lr = new LoginRequest(username, password);
        Request req = new Request(MessageType.LOGIN, JsonConverter.toJson(lr));

        client.send(req);
    }

    @FXML
    private void handleRegister() {
        cleanup(); // Gỡ listener trước khi rời đi
        SceneRouter.go("/view/Register.fxml", "Đăng ký tài khoản");
    }

    private void handleResponse(Response resp) {
        // Chỉ xử lý phản hồi liên quan đến LOGIN
        if (resp.getType() != MessageType.LOGIN) return;

        Platform.runLater(() -> {
            loginBtn.setDisable(false);

            if (resp.isSuccess()) {
                // Giải mã User từ dữ liệu JSON của server.
                // Note: resp.getData() do Gson trả về là một Map (LinkedTreeMap)
                // do User là abstract class. Re-serialize rồi parse theo role.
                String userJson = JsonConverter.toJson(resp.getData());
                // Đọc trước để xác định role
                User probe =
                        JsonConverter.fromJson(userJson, Admin.class);
                User user;
                if (probe != null && probe.getRole() == UserRole.ADMIN) {
                    user = probe;
                } else {
                    user = JsonConverter.fromJson(userJson, Member.class);
                }

                // Lưu vào Session Singleton
                Session.getInstance().setCurrentUser(user);

                statusLabel.setText("Đăng nhập thành công!");
                cleanup(); // Quan trọng: Gỡ listener để không bị lặp log ở màn hình sau

                // Chuyển hướng theo Role (Vai trò)
                redirectByRole(user);
            } else {
                statusLabel.setText("Lỗi: " + resp.getMessage());
            }
        });
    }

    private void redirectByRole(User user) {
        // Kiểm tra role dựa trên Enum Role em đã định nghĩa
        if (user.getRole() == UserRole.ADMIN) {
            SceneRouter.go("/view/AuctionList.fxml", "Quản trị viên");
        } else if (user.getRole() == UserRole.MEMBER) {
            // Đưa MEMBER vào UserDashboard (trung tâm điều hướng + ví tiền);
            // từ đó có thể chuyển sang AuctionList/AuctionDetail và SellerDashboard.
            SceneRouter.go("/view/UserDashboard.fxml", "Trang người dùng");
        } else {
            SceneRouter.go("/view/AuctionList.fxml", "Sàn đấu giá trực tuyến");
        }
    }

    private void cleanup() {
        client.removeListener(listener);
    }
}