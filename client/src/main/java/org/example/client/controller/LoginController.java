package org.example.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.session.Session;
import org.example.client.util.SceneRouter;
import org.example.model.user.Role;
import org.example.payload.MessageType;
import org.example.payload.dto.LoginRequest;

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
    private
}
