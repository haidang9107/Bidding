package org.example.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.util.SceneRouter;
import org.example.model.enums.MessageType;
import org.example.model.enums.UserRole;
import org.example.payload.Request;
import org.example.payload.Response;

/**
 * Controller cho màn hình Đăng ký.
 *
 * Gửi yêu cầu đăng ký lên server theo format payload (text):
 *     "username:password:email:role"
 * (Định dạng này khớp với AuthController.handleSignup ở phía server.)
 */
public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private ChoiceBox<String> roleBox;
    @FXML private Button registerBtn;
    @FXML private Button backBtn;
    @FXML private Label statusLabel;

    private final SocketClient client = SocketClient.getInstance();
    private ServerListener listener;

    @FXML
    public void initialize() {
        // Đổ danh sách vai trò vào ChoiceBox. Hệ thống chỉ có 2 role: MEMBER và ADMIN
        roleBox.setItems(FXCollections.observableArrayList(
                UserRole.MEMBER.name(),
                UserRole.ADMIN.name()
        ));
        roleBox.setValue(UserRole.MEMBER.name());

        // Đăng ký listener nhận phản hồi
        listener = this::handleResponse;
        client.addListener(listener);
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String email    = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmField.getText();
        String role     = roleBox.getValue();

        // Validate cơ bản phía client
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đủ username, email và mật khẩu!");
            return;
        }
        if (!password.equals(confirm)) {
            statusLabel.setText("Mật khẩu xác nhận không trùng khớp!");
            return;
        }
        if (password.length() < 4) {
            statusLabel.setText("Mật khẩu phải có ít nhất 4 ký tự!");
            return;
        }
        if (role == null || role.isEmpty()) {
            statusLabel.setText("Vui lòng chọn vai trò!");
            return;
        }

        registerBtn.setDisable(true);
        statusLabel.setText("Đang gửi yêu cầu đăng ký...");

        // Đóng gói payload đúng format mà server đang parse: "username:password:email:role"
        String payload = username + ":" + password + ":" + email + ":" + role;
        Request req = new Request(MessageType.SIGNUP, payload);
        client.send(req);
    }

    @FXML
    private void handleBack() {
        cleanup();
        SceneRouter.go("/view/Login.fxml", "Đăng nhập");
    }

    private void handleResponse(Response resp) {
        // Server có thể trả về type SUCCESS / ERROR khi xử lý SIGNUP.
        // Bỏ qua các message không liên quan (vd: BID_UPDATE broadcast).
        MessageType t = resp.getType();
        if (t != MessageType.SUCCESS && t != MessageType.ERROR && t != MessageType.SIGNUP) {
            return;
        }

        Platform.runLater(() -> {
            registerBtn.setDisable(false);

            if (resp.isSuccess()) {
                statusLabel.setText("Đăng ký thành công! Đang quay về đăng nhập...");
                cleanup();
                SceneRouter.go("/view/Login.fxml", "Đăng nhập");
            } else {
                statusLabel.setText("Lỗi: " + resp.getMessage());
            }
        });
    }

    private void cleanup() {
        client.removeListener(listener);
    }
}
