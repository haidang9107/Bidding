package org.example.client.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.Duration;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.notification.NotificationService;
import org.example.client.session.Session;
import org.example.client.util.SceneRouter;
import org.example.client.watchlist.MyBidsManager;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the realtime auction detail screen.
 *
 * Bug fixes:
 *  - {@code User#getUserId()} does not exist on the actual User model.
 *    We now identify the local user by {@code getAccountname()}, which is what
 *    the server's ProductResponse uses (winnerAccountname).
 *  - Bid placement payload still uses the "productId:bidderAccount:amount"
 *    format expected by the server's BidPlaceCommand.
 *
 * New integration:
 *  - On every successful bid the user is registered in {@link MyBidsManager}
 *    so they appear under the "Đang đấu giá của tôi" tab and receive
 *    outbid / win notifications.
 */
public class AuctionDetailController {

    // === FXML fields ===
    @FXML private Label itemNameLabel;
    @FXML private Label statusLabel;
    @FXML private Label countdownLabel;
    @FXML private Label categoryLabel;
    @FXML private Label itemDescLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label leaderLabel;
    @FXML private Label messageLabel;
    @FXML private LineChart<Number, Number> priceChart;

    @FXML private TextField bidAmountField;
    @FXML private Button bidBtn;

    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private Button autoBidBtn;

    @FXML private ListView<String> historyList;
    @FXML private Button backBtn;

    private final SocketClient client = SocketClient.getInstance();
    private ServerListener listener;
    private final ObservableList<String> historyData = FXCollections.observableArrayList();

    private int auctionId;
    private String productName;
    private long currentPrice = 0L;
    private long stepPrice = 0L;
    private String leaderAccount = null;
    private long startEpoch = 0L;
    private long endEpoch = 0L;

    private boolean autoBidEnabled = false;
    private long autoMaxBid = 0L;
    private long autoIncrement = 0L;

    private XYChart.Series<Number, Number> series;
    private Timeline countdownTimer;

    private final SimpleDateFormat timestampInFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat displayFmt = new SimpleDateFormat("HH:mm:ss");

    @FXML
    public void initialize() {
        historyList.setItems(historyData);
        series = new XYChart.Series<>();
        series.setName("Giá đấu");
        priceChart.getData().add(series);
        priceChart.setAnimated(false);
        priceChart.setLegendVisible(false);

        listener = this::handleResponse;
        client.addListener(listener);
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
        client.send(new Request(MessageType.PRODUCT_DETAIL, String.valueOf(auctionId)));
    }

    @FXML
    private void handleBid() {
        User u = Session.getInstance().getCurrentUser();
        if (u == null) {
            alert(Alert.AlertType.ERROR, "Chưa đăng nhập!");
            return;
        }
        String amountStr = bidAmountField.getText() == null ? "" : bidAmountField.getText().trim();
        if (amountStr.isEmpty()) {
            messageLabel.setText("Hãy nhập số tiền!");
            return;
        }
        long amount;
        try { amount = Long.parseLong(amountStr); }
        catch (NumberFormatException ex) {
            messageLabel.setText("Số tiền không hợp lệ!");
            return;
        }
        long min = currentPrice + stepPrice;
        if (amount < min) {
            messageLabel.setText("Phải đặt ít nhất " + min);
            return;
        }
        sendBid(amount);
    }

    @FXML
    private void handleAutoBid() {
        try {
            long max = Long.parseLong(maxBidField.getText().trim());
            long inc = Long.parseLong(incrementField.getText().trim());
            if (max <= 0 || inc <= 0) {
                messageLabel.setText("Max và bước phải > 0");
                return;
            }
            autoMaxBid = max;
            autoIncrement = inc;
            autoBidEnabled = true;
            messageLabel.setText("Auto-bid bật: max=" + max + ", step=" + inc);
        } catch (Exception ex) {
            messageLabel.setText("Auto-bid: nhập số!");
        }
    }

    @FXML
    private void handleBack() {
        cleanup();
        SceneRouter.go("/view/AuctionList.fxml", "Sàn đấu giá trực tuyến");
    }

    /**
     * Send bid. Format: "productId:bidderAccountname:amount"
     */
    private void sendBid(long amount) {
        User u = Session.getInstance().getCurrentUser();
        if (u == null) return;
        String payload = auctionId + ":" + u.getAccountname() + ":" + amount;
        bidBtn.setDisable(true);
        client.send(new Request(MessageType.BID_PLACE, payload));

        // Optimistically register in MyBidsManager so the user sees the entry
        // immediately under "Đang đấu giá của tôi".
        MyBidsManager.getInstance().recordMyBid(auctionId, productName, amount);

        new Thread(() -> {
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> bidBtn.setDisable(false));
        }, "bid-cooldown").start();
    }

    private void handleResponse(Response resp) {
        MessageType t = resp.getType();
        if (t == null) return;

        switch (t) {
            case PRODUCT_DETAIL -> Platform.runLater(() -> applyProductData(resp, false));
            case BID_UPDATE     -> Platform.runLater(() -> applyProductData(resp, true));
            case SUCCESS        -> Platform.runLater(() ->
                    messageLabel.setText("✓ " + (resp.getMessage() == null ? "OK" : resp.getMessage())));
            case ERROR          -> Platform.runLater(() ->
                    messageLabel.setText("✗ " + (resp.getMessage() == null ? "Lỗi" : resp.getMessage())));
            case AUCTION_END    -> Platform.runLater(() -> {
                statusLabel.setText("FINISHED");
                messageLabel.setText("Phiên đã kết thúc!");
                stopCountdown();
                // Forward to MyBidsManager so a win toast can fire if applicable
                User u = Session.getInstance().getCurrentUser();
                String me = u == null ? null : u.getAccountname();
                MyBidsManager.getInstance().onAuctionEnd(auctionId, leaderAccount, currentPrice, me);
            });
            default -> { /* ignore */ }
        }
    }

    private void applyProductData(Response resp, boolean isBroadcast) {
        if (!resp.isSuccess() || resp.getData() == null) return;

        String raw = JsonConverter.toJson(resp.getData());
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> m;
        try { m = new Gson().fromJson(raw, mapType); }
        catch (Exception ex) { m = new HashMap<>(); }
        if (m == null) return;

        int pid = readInt(m.get("productId"));
        if (isBroadcast && pid != auctionId) return;
        if (pid != 0 && this.auctionId == 0) this.auctionId = pid;

        String name = readStr(m.get("productName"));
        if (name == null || name.isEmpty()) name = readStr(m.get("name"));
        String desc = readStr(m.get("description"));
        String cat = readStr(m.get("category"));
        String status = readStr(m.get("status"));
        long cur = readLong(m.get("currentPrice"));
        long step = readLong(m.get("stepPrice"));
        String winner = readStr(m.get("winnerAccountname"));

        long startTs = readEpoch(m.get("startTime"));
        long endTs = readEpoch(m.get("endTime"));

        boolean priceChanged = (cur != currentPrice);
        String oldLeader = this.leaderAccount;
        this.currentPrice = cur;
        this.stepPrice = step;
        this.leaderAccount = (winner == null || winner.isEmpty()) ? null : winner;
        if (name != null && !name.isEmpty()) this.productName = name;
        if (startTs > 0) this.startEpoch = startTs;
        if (endTs > 0) this.endEpoch = endTs;

        itemNameLabel.setText(name == null || name.isEmpty() ? "Sản phẩm #" + pid : name);
        itemDescLabel.setText(desc == null ? "" : desc);
        categoryLabel.setText("Loại: " + cat);
        statusLabel.setText(status);
        currentPriceLabel.setText(formatPrice(cur));
        leaderLabel.setText(leaderAccount == null ? "(Chưa có)" : leaderAccount);

        if (isBroadcast && priceChanged) {
            String line = String.format("[%s] %s đặt: %s",
                    displayFmt.format(new Date()),
                    leaderAccount == null ? "?" : leaderAccount,
                    formatPrice(cur));
            historyData.add(0, line);
            addChartPoint(cur);

            // Notify MyBidsManager so the user sees status changes globally
            User u = Session.getInstance().getCurrentUser();
            String me = u == null ? null : u.getAccountname();
            MyBidsManager.getInstance().onPriceUpdate(auctionId, productName, cur, leaderAccount, me);

            triggerAutoBidIfNeeded();
        }

        if (!isBroadcast && series.getData().isEmpty()) {
            addChartPoint(cur);
        }

        startCountdownIfNeeded();
    }

    private void triggerAutoBidIfNeeded() {
        if (!autoBidEnabled) return;
        User u = Session.getInstance().getCurrentUser();
        if (u == null) return;
        // If we are the current leader, no need to auto-bid
        if (leaderAccount != null && leaderAccount.equalsIgnoreCase(u.getAccountname())) return;
        long next = currentPrice + Math.max(stepPrice, autoIncrement);
        if (next > autoMaxBid) {
            messageLabel.setText("Auto-bid dừng: vượt max " + autoMaxBid);
            autoBidEnabled = false;
            NotificationService.getInstance().info(
                    "Auto-bid đã dừng",
                    "Giá vượt mức tối đa bạn cài (" + formatPrice(autoMaxBid) + ").");
            return;
        }
        new Thread(() -> {
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> sendBid(next));
        }, "auto-bid").start();
    }

    private void addChartPoint(long price) {
        long base = startEpoch > 0 ? startEpoch : System.currentTimeMillis();
        long elapsed = (System.currentTimeMillis() - base) / 1000;
        series.getData().add(new XYChart.Data<>(elapsed, price));
    }

    private void startCountdownIfNeeded() {
        if (countdownTimer != null) return;
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickCountdown()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
        tickCountdown();
    }

    private void tickCountdown() {
        if (endEpoch <= 0) {
            countdownLabel.setText("--:--");
            return;
        }
        long remain = (endEpoch - System.currentTimeMillis()) / 1000;
        if (remain <= 0) {
            countdownLabel.setText("00:00:00");
            stopCountdown();
            return;
        }
        long h = remain / 3600;
        long mn = (remain % 3600) / 60;
        long s = remain % 60;
        countdownLabel.setText(String.format("%02d:%02d:%02d", h, mn, s));
    }

    private void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
    }

    private void cleanup() {
        stopCountdown();
        client.removeListener(listener);
    }

    private void alert(Alert.AlertType type, String msg) {
        new Alert(type, msg).showAndWait();
    }

    private String formatPrice(long p) {
        return String.format("%,d đ", p);
    }

    private int readInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }

    private long readLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    private String readStr(Object o) {
        return o == null ? "" : o.toString();
    }

    private long readEpoch(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        try {
            Date d = timestampInFmt.parse(o.toString());
            return d.getTime();
        } catch (Exception e) {
            return 0L;
        }
    }
}
