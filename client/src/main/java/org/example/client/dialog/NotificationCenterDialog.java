package org.example.client.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.client.notification.NotificationService;
import org.example.client.notification.NotificationService.Notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * "Hộp thông báo" — lists the notifications the user received this session
 * (outbid alerts, wins, wallet changes, system messages). Backed by
 * {@link NotificationService#getHistory()} which keeps the latest 100.
 *
 * <p>Client-side only: the server has no notification-history endpoint, so
 * this persists for the lifetime of the app process. Newest first.</p>
 *
 * <p>Styled by {@code /css/notification.css}, attached explicitly because a
 * Dialog does not inherit the main Scene's stylesheets.</p>
 */
public final class NotificationCenterDialog {

    private NotificationCenterDialog() {}

    public static void show() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Thông báo");
        dlg.setHeaderText("Thông báo của bạn (phiên làm việc này)");
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.getDialogPane().setMinWidth(540);

        // Dialogs don't inherit the app Scene's stylesheets — attach ours so
        // the notification center matches the dark theme (đồng bộ giao diện).
        try {
            dlg.getDialogPane().getStylesheets().add(
                    NotificationCenterDialog.class
                            .getResource("/css/notification.css").toExternalForm());
        } catch (Exception ignored) {}
        dlg.getDialogPane().getStyleClass().add("notif-pane");

        VBox listBox = new VBox(8);
        listBox.setPadding(new Insets(6));

        List<Notification> history = NotificationService.getInstance().getHistory();
        if (history.isEmpty()) {
            Label empty = new Label("Chưa có thông báo nào trong phiên này.");
            empty.getStyleClass().add("notif-empty");
            empty.setPadding(new Insets(18));
            listBox.getChildren().add(empty);
        } else {
            SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss dd/MM");
            for (Notification n : history) {
                listBox.getChildren().add(buildItem(n, fmt));
            }
        }

        ScrollPane sp = new ScrollPane(listBox);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(400);
        sp.getStyleClass().add("notif-scroll");
        dlg.getDialogPane().setContent(sp);
        dlg.showAndWait();
    }

    private static HBox buildItem(Notification n, SimpleDateFormat fmt) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.getStyleClass().add("notif-row");
        switch (n.kind) {
            case WIN    -> row.getStyleClass().add("notif-win");
            case OUTBID -> row.getStyleClass().add("notif-outbid");
            case ERROR  -> row.getStyleClass().add("notif-error");
            default     -> { /* default accent */ }
        }

        String icon = switch (n.kind) {
            case WIN    -> "🏆";
            case OUTBID -> "⚠";
            case ERROR  -> "✗";
            default     -> "ℹ";
        };
        Label iconL = new Label(icon);
        iconL.getStyleClass().add("notif-icon");

        VBox text = new VBox(2);
        Label title = new Label(n.title == null ? "" : n.title);
        title.getStyleClass().add("notif-title");
        Label body = new Label(n.body == null ? "" : n.body);
        body.setWrapText(true);
        body.getStyleClass().add("notif-body");
        text.getChildren().addAll(title, body);
        HBox.setHgrow(text, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        Label time = new Label(fmt.format(new Date(n.timestampMillis)));
        time.getStyleClass().add("notif-time");

        row.getChildren().addAll(iconL, text, spacer, time);
        return row;
    }
}
