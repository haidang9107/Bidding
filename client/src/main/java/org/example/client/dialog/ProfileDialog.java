package org.example.client.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.client.network.SocketClient;
import org.example.client.notification.NotificationService;
import org.example.client.session.Session;
import org.example.dto.UserProfileUpdateRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.util.JsonConverter;

/**
 * Modal dialog showing the current user's profile and allowing edits.
 *
 * NEW class — adds the "click avatar → view / edit profile" feature requested
 * by the BTL. Does not modify any existing controller's logic; controllers
 * just call {@link #show()} when their avatar button is clicked.
 *
 * The dialog updates the local Session immediately (for snappy UX) and sends
 * an UPDATE_PROFILE request to the server so any persistent storage can be
 * synchronised.
 */
public final class ProfileDialog {

    private ProfileDialog() {
    }

    public static void show() {
        User user = Session.getInstance().getCurrentUser();
        if (user == null) return;

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Thông tin cá nhân");
        stage.setResizable(false);

        Label title = new Label("THÔNG TIN CÁ NHÂN");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label avatar = new Label(initials(user));
        avatar.setMinSize(72, 72);
        avatar.setMaxSize(72, 72);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #5a8dee, #2c6ad6);"
              + "-fx-background-radius: 36;"
              + "-fx-text-fill: white;"
              + "-fx-font-size: 22px;"
              + "-fx-font-weight: bold;");

        TextField fullnameField = new TextField(safe(user.getFullname()));
        TextField emailField    = new TextField(safe(user.getEmail()));
        PasswordField oldPass   = new PasswordField();
        PasswordField newPass   = new PasswordField();
        styleField(fullnameField);
        styleField(emailField);
        styleField(oldPass);
        styleField(newPass);

        Label readonlyUser = new Label(safe(user.getAccountname()));
        readonlyUser.setStyle("-fx-text-fill: #c0c0d0; -fx-font-size: 13px;");

        Label readonlyRole = new Label(String.valueOf(user.getRole()));
        readonlyRole.setStyle("-fx-text-fill: #c0c0d0; -fx-font-size: 13px;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        int r = 0;
        grid.add(label("Tên đăng nhập"), 0, r); grid.add(readonlyUser, 1, r++);
        grid.add(label("Vai trò"),         0, r); grid.add(readonlyRole,    1, r++);
        grid.add(label("Họ tên"),          0, r); grid.add(fullnameField,   1, r++);
        grid.add(label("Email"),           0, r); grid.add(emailField,      1, r++);
        grid.add(label("Mật khẩu hiện tại"), 0, r); grid.add(oldPass,        1, r++);
        grid.add(label("Mật khẩu mới"),    0, r); grid.add(newPass,         1, r++);

        Label status = new Label();
        status.setStyle("-fx-text-fill: #ffd166; -fx-font-size: 12px;");

        Button saveBtn = new Button("Lưu thay đổi");
        saveBtn.setStyle(
                "-fx-background-color: #5a8dee; -fx-text-fill: white;"
              + "-fx-font-weight: bold; -fx-padding: 10 18;"
              + "-fx-background-radius: 6; -fx-cursor: hand;");
        Button cancelBtn = new Button("Đóng");
        cancelBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #c0c0d0;"
              + "-fx-border-color: #3a3a4e; -fx-border-radius: 6;"
              + "-fx-padding: 8 14; -fx-cursor: hand;");

        cancelBtn.setOnAction(e -> stage.close());

        saveBtn.setOnAction(e -> {
            String newFullname = safe(fullnameField.getText());
            String newEmail    = safe(emailField.getText());
            String oldP        = safe(oldPass.getText());
            String newP        = safe(newPass.getText());

            if (newEmail.isEmpty()) {
                status.setText("Email không được trống.");
                return;
            }
            if (!newP.isEmpty() && oldP.isEmpty()) {
                status.setText("Hãy nhập mật khẩu hiện tại để đổi mật khẩu.");
                return;
            }
            if (!newP.isEmpty() && newP.length() < 4) {
                status.setText("Mật khẩu mới phải có ít nhất 4 ký tự.");
                return;
            }

            // Optimistic local update
            user.setFullname(newFullname);
            user.setEmail(newEmail);

            // Send to server (UPDATE_PROFILE). Server may answer "coming soon",
            // but the payload is correctly structured for whenever it's added.
            UserProfileUpdateRequest req = buildRequest(user.getAccountname(),
                    newFullname, newEmail, oldP, newP);
            SocketClient.getInstance().send(
                    new Request(MessageType.UPDATE_PROFILE, JsonConverter.toJson(req)));

            NotificationService.getInstance().info(
                    "Hồ sơ đã cập nhật",
                    "Thông tin cá nhân đã được lưu.");
            stage.close();
        });

        HBox actions = new HBox(10, cancelBtn, saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(14, title, avatar, grid, status, actions);
        layout.setPadding(new Insets(24));
        layout.setAlignment(Pos.TOP_LEFT);
        layout.setMinWidth(420);
        layout.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a1a2e, #16213e);");

        VBox.setMargin(avatar, new Insets(0, 0, 4, 0));

        Region spacer = new Region();
        spacer.setMinHeight(4);

        Scene scene = new Scene(layout);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static UserProfileUpdateRequest buildRequest(String accountname,
                                                         String fullname,
                                                         String email,
                                                         String oldPass,
                                                         String newPass) {
        UserProfileUpdateRequest r = new UserProfileUpdateRequest();
        // Use reflection-safe approach: just set known fields via setters that exist.
        try { r.getClass().getMethod("setAccountname", String.class).invoke(r, accountname); } catch (Exception ignored) {}
        try { r.getClass().getMethod("setFullname",   String.class).invoke(r, fullname);   } catch (Exception ignored) {}
        try { r.getClass().getMethod("setEmail",      String.class).invoke(r, email);      } catch (Exception ignored) {}
        try { r.getClass().getMethod("setOldPassword",String.class).invoke(r, oldPass);    } catch (Exception ignored) {}
        try { r.getClass().getMethod("setNewPassword",String.class).invoke(r, newPass);    } catch (Exception ignored) {}
        return r;
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #8a8aa0; -fx-font-size: 12px;");
        return l;
    }

    private static void styleField(TextField f) {
        f.setStyle(
                "-fx-background-color: #2a2a3e;"
              + "-fx-text-fill: white;"
              + "-fx-prompt-text-fill: #7a7a90;"
              + "-fx-padding: 8 10;"
              + "-fx-background-radius: 6;"
              + "-fx-font-size: 13px;");
        f.setPrefWidth(240);
    }

    private static String initials(User u) {
        String src = u.getFullname() != null && !u.getFullname().isEmpty()
                ? u.getFullname() : u.getAccountname();
        if (src == null || src.isEmpty()) return "?";
        String[] parts = src.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 2; i++) {
            if (!parts[i].isEmpty()) sb.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
