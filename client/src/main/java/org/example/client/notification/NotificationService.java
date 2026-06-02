package org.example.client.notification;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.client.util.SceneRouter;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-app toast / notification service.
 *
 * Single, in-memory only — does not modify any existing controller logic.
 *
 * Two purposes:
 *  1. Show transient toast popups (top-right) for events like "Bạn đã bị vượt giá!"
 *     or "Chúc mừng, bạn đã thắng phiên đấu giá!".
 *  2. Maintain a small list of pending notifications subscribed by listeners so
 *     other screens (UserDashboard, AuctionList) can show in-app banners /
 *     unread counts as the user navigates.
 *
 * Singleton.
 */
public final class NotificationService {

    private static final NotificationService INSTANCE = new NotificationService();

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    /** A simple notification event held by the service. */
    public static final class Notification {
        public final String title;
        public final String body;
        public final Kind kind;
        public final long timestampMillis;
        public Notification(String title, String body, Kind kind) {
            this.title = title;
            this.body  = body;
            this.kind  = kind;
            this.timestampMillis = System.currentTimeMillis();
        }
    }

    public enum Kind { INFO, OUTBID, WIN, ERROR }

    private final CopyOnWriteArrayList<Notification> history = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Notification>> subscribers = new CopyOnWriteArrayList<>();

    private NotificationService() {
    }

    // ============================================================
    // Public API
    // ============================================================
    public void info(String title, String body) {
        push(new Notification(title, body, Kind.INFO));
    }

    public void outbid(String title, String body) {
        push(new Notification(title, body, Kind.OUTBID));
    }

    public void win(String title, String body) {
        push(new Notification(title, body, Kind.WIN));
    }

    public void error(String title, String body) {
        push(new Notification(title, body, Kind.ERROR));
    }

    public void subscribe(Consumer<Notification> listener) {
        subscribers.add(listener);
    }

    public void unsubscribe(Consumer<Notification> listener) {
        subscribers.remove(listener);
    }

    /** Read-only snapshot of recent notifications. */
    public java.util.List<Notification> getHistory() {
        return java.util.Collections.unmodifiableList(history);
    }

    // ============================================================
    // Internals
    // ============================================================
    private void push(Notification n) {
        history.add(0, n);
        if (history.size() > 100) {
            history.remove(history.size() - 1);
        }
        // Notify subscribers + show toast on FX thread.
        Runnable r = () -> {
            for (Consumer<Notification> s : subscribers) {
                try { s.accept(n); } catch (Exception ignored) {}
            }
            showToast(n);
        };
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    private void showToast(Notification n) {
        Stage stage = SceneRouter.getPrimaryStage();
        if (stage == null || !stage.isShowing()) return;

        Popup popup = new Popup();
        popup.setAutoFix(true);

        Label titleLabel = new Label(n.title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.WHITE);

        Label bodyLabel = new Label(n.body == null ? "" : n.body);
        bodyLabel.setTextFill(Color.web("#e8e8f5"));
        bodyLabel.setFont(Font.font(12));
        bodyLabel.setWrapText(true);
        bodyLabel.setMaxWidth(340);

        VBox content = new VBox(4, titleLabel, bodyLabel);
        content.setStyle(buildStyle(n.kind));
        content.setMaxWidth(360);

        StackPane wrap = new StackPane(content);
        wrap.setAlignment(Pos.CENTER);
        wrap.setStyle("-fx-background-color: transparent;");

        popup.getContent().add(wrap);

        // Position to top-right of the stage.
        double targetX = stage.getX() + stage.getWidth() - 380;
        double targetY = stage.getY() + 60;
        popup.show(stage, targetX, targetY);

        // Fade-in / hold / fade-out then hide.
        wrap.setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(220), wrap);
        in.setFromValue(0); in.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.seconds(3.5));
        FadeTransition out = new FadeTransition(Duration.millis(300), wrap);
        out.setFromValue(1); out.setToValue(0);

        SequentialTransition seq = new SequentialTransition(in, hold, out);
        seq.setOnFinished(e -> popup.hide());
        seq.play();
    }

    private static String buildStyle(Kind kind) {
        String bg;
        String border;
        switch (kind) {
            case OUTBID -> { bg = "#5a2a2a"; border = "#ff8080"; }
            case WIN    -> { bg = "#1f4a2e"; border = "#67e8b1"; }
            case ERROR  -> { bg = "#4a1f1f"; border = "#ff6666"; }
            default     -> { bg = "#23233a"; border = "#5a8dee"; }
        }
        return "-fx-background-color: " + bg + ";"
             + "-fx-background-radius: 10;"
             + "-fx-border-color: " + border + ";"
             + "-fx-border-radius: 10;"
             + "-fx-border-width: 1;"
             + "-fx-padding: 12 16;"
             + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 14, 0, 0, 6);";
    }
}
