package org.example.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.util.SceneRouter;
import org.example.dto.request.SignupRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

/**
 * Controller for the Register screen.
 *
 * Changes (per BTL requirements):
 *  - The role choice has been removed; every account is registered as MEMBER.
 *  - Payload is now sent as a SignupRequest JSON (matches server's SignupCommand /
 *    AuthService), instead of the legacy "username:password:email:role" colon
 *    format that no longer matches the server contract.
 */
public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Button registerBtn;
    @FXML private Button backBtn;
    @FXML private Label statusLabel;

    private final SocketClient client = SocketClient.getInstance();
    private ServerListener listener;

    @FXML
    public void initialize() {
        listener = this::handleResponse;
        client.addListener(listener);
    }

    @FXML
    private void handleRegister() {
        String username = safe(usernameField.getText());
        String email    = safe(emailField.getText());
        String password = passwordField.getText();
        String confirm  = confirmField.getText();

        // Basic client-side validation
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

        registerBtn.setDisable(true);
        statusLabel.setText("Đang gửi yêu cầu đăng ký...");

        // Server (AuthController.handleSignup) expects a SignupRequest JSON object.
        // No role is sent: server always creates MEMBER.
        SignupRequest signup = new SignupRequest(username, password, email);
        Request req = new Request(MessageType.SIGNUP, signup);
        client.send(req);
    }

    @FXML
    private void handleBack() {
        cleanup();
        SceneRouter.go("/view/Login.fxml", "Đăng nhập");
    }

    private void handleResponse(Response resp) {
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

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
