package org.example.client.network;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class CreateAccountController {

    @FXML private StackPane leftPanel;
    @FXML private ImageView bgImage;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox termsCheck;
    @FXML private Button togglePasswordBtn;
    @FXML private Button createBtn;

    @FXML
    public void initialize() {
        // Giả sử bạn đang load ảnh hoặc CSS
        var resource = getClass().getResource("/css/create-account.css");

        if (resource == null) {
            System.out.println("LỖI: Không tìm thấy file! Kiểm tra lại đường dẫn.");
        } else {
            String path = resource.toExternalForm();
            // Tiếp tục xử lý...
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        System.out.println("Chuyển sang màn hình Đăng nhập");
        // TODO: load Login.fxml
    }

    @FXML
    private void handleTogglePassword(ActionEvent event) {
        // TODO: swap PasswordField <-> TextField để hiện/ẩn mật khẩu
        System.out.println("Toggle mật khẩu");
    }

    @FXML
    private void handleCreateAccount(ActionEvent event) {
        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String email     = emailField.getText().trim();
        String password  = passwordField.getText();
        boolean agreed   = termsCheck.isSelected();

        if (firstName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng điền đầy đủ thông tin.");
            return;
        }
        if (!agreed) {
            showAlert(Alert.AlertType.WARNING, "Điều khoản", "Vui lòng đồng ý với điều khoản dịch vụ.");
            return;
        }
        if (!email.contains("@")) {
            showAlert(Alert.AlertType.ERROR, "Email không hợp lệ", "Vui lòng nhập email hợp lệ.");
            return;
        }
        if (password.length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Mật khẩu yếu", "Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }

        System.out.println("Tạo tài khoản: " + firstName + " " + lastName + " | " + email);
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tài khoản đã được tạo thành công!");
    }

    @FXML
    private void handleGoogle(ActionEvent event) {
        System.out.println("Đăng ký bằng Google");
    }

    @FXML
    private void handleApple(ActionEvent event) {
        System.out.println("Đăng ký bằng Apple");
    }

    @FXML
    private void handleTerms(ActionEvent event) {
        System.out.println("Mở Điều khoản dịch vụ");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
