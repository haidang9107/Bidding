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
import java.util.List;
import java.util.ArrayList;
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
    @FXML private Button cancelAutoBidBtn;

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

    /** When true the user is only viewing (watching) the room, not
     *  participating — bidding controls are hidden and we don't JOIN. */
    private boolean viewOnly = false;

    public void setAuctionId(int auctionId) {
        setAuctionId(auctionId, false);
    }

    /**
     * @param viewOnly if true, the user only watches: we skip JOIN_AUCTION_ROOM
     *                 (so the server won't count them as a participant) but
     *                 still fetch the snapshot and listen for broadcasts to
     *                 keep the view live. Bidding controls are disabled.
     */
    public void setAuctionId(int auctionId, boolean viewOnly) {
        this.auctionId = auctionId;
        this.viewOnly = viewOnly;
        // Always JOIN the room — even in view-only mode. The server only
        // broadcasts BID_UPDATE / TIMER_TICK / AUCTION_END to clients that
        // are in the room (RoomManager). If a watcher didn't join, the price
        // chart would never update live, defeating the purpose of "Xem phòng".
        // For a watcher we simply disable the bidding controls below; joining
        // a room is read-only on the server side (it doesn't place any bid).
        client.send(new Request(MessageType.JOIN_AUCTION_ROOM,
                new org.example.dto.request.AuctionRoomRequest(auctionId)));
        // Fetch the initial snapshot.
        client.send(new Request(MessageType.PRODUCT_DETAIL,
                new org.example.dto.request.AuctionRoomRequest(auctionId)));

        // Fetch the FULL bid history so the chart can replay every past bid,
        // not just the bids that happen while we're watching. Server stores
        // every bid in the `bids` table and returns it via BID_HISTORY as a
        // PagedResponse<Bid>. We ask for a large page to get them all at once.
        client.send(new Request(MessageType.BID_HISTORY,
                new org.example.dto.request.BidHistoryRequest(auctionId, 1, 1000)));

        if (viewOnly) {
            // Disable bidding controls — best-effort, only if the fields exist.
            Platform.runLater(() -> {
                if (bidBtn != null) { bidBtn.setDisable(true); bidBtn.setText("Chế độ xem"); }
                if (bidAmountField != null) bidAmountField.setDisable(true);
                if (autoBidBtn != null) autoBidBtn.setDisable(true);
                if (cancelAutoBidBtn != null) cancelAutoBidBtn.setDisable(true);
            });
        }
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
            // Server's AutoBidSetCommand: payload is AutoBidRequest
            // {auctionId, maxBid, incrementAmount}. Server stores the config
            // and bids on the user's behalf when outbid, up to maxBid.
            org.example.dto.request.AutoBidRequest req =
                    new org.example.dto.request.AutoBidRequest(auctionId, max, inc);
            client.send(new Request(MessageType.AUTO_BID_SET, req));
            autoMaxBid = max;
            autoIncrement = inc;
            autoBidEnabled = true;
            messageLabel.setText("Auto-bid bật: max=" + max + ", step=" + inc);
        } catch (Exception ex) {
            messageLabel.setText("Auto-bid: nhập số!");
        }
    }

    /**
     * Cancels server-side auto-bid for this auction. Uses
     * MessageType.AUTO_BID_CANCEL (server's AutoBidCancelCommand removes the
     * config so the server stops bidding for us). This handler can be wired
     * to a separate "Hủy auto-bid" button in the FXML.
     */
    @FXML
    private void handleCancelAutoBid() {
        if (!autoBidEnabled) {
            messageLabel.setText("Chưa bật auto-bid.");
            return;
        }
        org.example.dto.request.AutoBidRequest req =
                new org.example.dto.request.AutoBidRequest(auctionId,
                        autoMaxBid, autoIncrement);
        client.send(new Request(MessageType.AUTO_BID_CANCEL, req));
        autoBidEnabled = false;
        autoMaxBid = 0L;
        autoIncrement = 0L;
        messageLabel.setText("Đã hủy auto-bid.");
    }

    @FXML
    private void handleBack() {
        cleanup();
        SceneRouter.go("/view/AuctionList.fxml", "Sàn đấu giá trực tuyến");
    }

    @FXML
    private void handleGoHome() {
        cleanup();
        SceneRouter.go("/view/UserDashboard.fxml", "Trang người dùng");
    }

    /**
     * Send a bid to the server. The server's BidPlaceCommand expects a
     * BidRequest{auctionId, amount} DTO; the bidder identity is taken from
     * the authenticated session. The previous colon-separated String format
     * silently failed server-side because Gson cannot map a String to a DTO.
     */
    private void sendBid(long amount) {
        User u = Session.getInstance().getCurrentUser();
        if (u == null) return;
        bidBtn.setDisable(true);
        org.example.dto.request.BidRequest req =
                new org.example.dto.request.BidRequest(auctionId, amount);
        client.send(new Request(MessageType.BID_PLACE, req));

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
            case BID_UPDATE     -> Platform.runLater(() -> applyBidUpdate(resp));
            case BID_HISTORY    -> Platform.runLater(() -> applyBidHistory(resp));
            case TIMER_TICK     -> Platform.runLater(() -> applyTimerTick(resp));
            case NOTIFICATION   -> Platform.runLater(() -> {
                String body = resp.getMessage() == null ? "Thông báo" : resp.getMessage();
                NotificationService.getInstance().info("Thông báo", body);
            });
            case SUCCESS        -> Platform.runLater(() -> {
                String body = resp.getMessage() == null
                        ? "Đặt giá thành công." : resp.getMessage();
                NotificationService.getInstance().info("✓ Đặt giá", body);
                messageLabel.setText("");
            });
            case ERROR          -> Platform.runLater(() -> {
                String body = resp.getMessage() == null ? "Lỗi" : resp.getMessage();
                NotificationService.getInstance().error("✗ Lỗi", body);
                messageLabel.setText("");
            });
            case AUCTION_END    -> Platform.runLater(() -> applyAuctionEnd(resp));
            default -> { /* ignore */ }
        }
    }

    /**
     * Realtime bid update. Server payload is BidUpdateNotify
     * { auctionId, bidderAccountname, amount, autoBidApplied, newEndTime }.
     * Reads those exact fields (the previous code looked for productId/
     * currentPrice which don't exist on this DTO, so prices never updated
     * live — the user had to leave and re-enter the room).
     */
    @SuppressWarnings("unchecked")
    private void applyBidUpdate(Response resp) {
        if (!resp.isSuccess() || resp.getData() == null) return;
        String raw = JsonConverter.toJson(resp.getData());
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> m;
        try { m = new Gson().fromJson(raw, mapType); }
        catch (Exception ex) { return; }
        if (m == null) return;

        int aId = readInt(m.get("auctionId"));
        if (aId != 0 && aId != auctionId) return;   // not our room
        long amount   = readLong(m.get("amount"));
        String bidder = readStr(m.get("bidderAccountname"));
        long newEnd   = readEpoch(m.get("newEndTime"));

        boolean changed = amount != currentPrice || !equalsSafe(bidder, leaderAccount);
        this.currentPrice = amount;
        this.leaderAccount = (bidder == null || bidder.isEmpty()) ? null : bidder;
        if (newEnd > 0) this.endEpoch = newEnd;

        currentPriceLabel.setText(formatPrice(amount));
        leaderLabel.setText(leaderAccount == null ? "(Chưa có)" : leaderAccount);

        if (changed) {
            String line = String.format("[%s] %s đặt: %s",
                    displayFmt.format(new Date()),
                    leaderAccount == null ? "?" : leaderAccount,
                    formatPrice(amount));
            historyData.add(0, line);
            addChartPoint(amount);

            User u = Session.getInstance().getCurrentUser();
            String me = u == null ? null : u.getAccountname();
            MyBidsManager.getInstance().onPriceUpdate(auctionId, productName, amount, leaderAccount, me);

            // Toast when someone else outbids — but not for our own bid.
            if (me != null && leaderAccount != null && !me.equals(leaderAccount)) {
                NotificationService.getInstance().info("Có người trả giá cao hơn",
                        leaderAccount + " vừa đặt " + formatPrice(amount));
            }
            triggerAutoBidIfNeeded();
        }
        startCountdownIfNeeded();
    }

    /** Server TIMER_TICK — optional remaining-time push. Best-effort. */
    @SuppressWarnings("unchecked")
    private void applyTimerTick(Response resp) {
        if (resp.getData() == null) return;
        try {
            String raw = JsonConverter.toJson(resp.getData());
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> m = new Gson().fromJson(raw, mapType);
            if (m == null) return;
            long endTs = readEpoch(m.get("endTime"));
            if (endTs > 0) { this.endEpoch = endTs; startCountdownIfNeeded(); }
        } catch (Exception ignored) {}
    }

    /**
     * Auction finished. Server payload is AuctionEndNotify
     * { auctionId, winnerAccountname, finalPrice, productName, productDetail }.
     */
    @SuppressWarnings("unchecked")
    private void applyAuctionEnd(Response resp) {
        statusLabel.setText("FINISHED");
        stopCountdown();
        String winner = null;
        long finalPrice = currentPrice;
        if (resp.getData() != null) {
            try {
                String raw = JsonConverter.toJson(resp.getData());
                Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> m = new Gson().fromJson(raw, mapType);
                if (m != null) {
                    winner = readStr(m.get("winnerAccountname"));
                    long fp = readLong(m.get("finalPrice"));
                    if (fp > 0) finalPrice = fp;
                }
            } catch (Exception ignored) {}
        }
        this.currentPrice = finalPrice;
        this.leaderAccount = winner;
        currentPriceLabel.setText(formatPrice(finalPrice));
        leaderLabel.setText(winner == null ? "(Không có người thắng)" : winner);

        User u = Session.getInstance().getCurrentUser();
        String me = u == null ? null : u.getAccountname();
        MyBidsManager.getInstance().onAuctionEnd(auctionId, winner, finalPrice, me);

        if (winner != null && winner.equals(me)) {
            messageLabel.setText("");
            NotificationService.getInstance().win("🎉 Chúc mừng!",
                    "Bạn đã thắng phiên đấu giá với giá " + formatPrice(finalPrice)
                  + ". Sản phẩm đã vào kho của bạn.");
        } else if (winner != null) {
            messageLabel.setText("Phiên đã kết thúc. Người thắng: " + winner);
            NotificationService.getInstance().info("Phiên kết thúc",
                    "Người thắng: " + winner);
        } else {
            messageLabel.setText("Phiên đã kết thúc, không có người thắng.");
            NotificationService.getInstance().info("Phiên kết thúc",
                    "Không có người thắng.");
        }
    }

    private static boolean equalsSafe(String a, String b) {
        return a == null ? b == null : a.equals(b);
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

    /** Plot a historical bid at its real timestamp (seconds since auction
     *  start) rather than "now". */
    private void addHistoricChartPoint(long price, long bidEpochMs) {
        long base = startEpoch > 0 ? startEpoch : bidEpochMs;
        long elapsed = Math.max(0, (bidEpochMs - base) / 1000);
        series.getData().add(new XYChart.Data<>(elapsed, price));
    }

    /**
     * Replays the full bid history into the chart and the history list when we
     * enter a room. Server returns a PagedResponse&lt;Bid&gt; where each Bid is
     * { auctionId, bidderAccountname, bidAmount, bidTime }. Without this, the
     * chart only showed the single current price plus whatever bids arrived
     * live — so a user joining late saw an almost-empty chart even though the
     * auction had many past bids.
     */
    @SuppressWarnings("unchecked")
    private void applyBidHistory(Response resp) {
        if (!resp.isSuccess() || resp.getData() == null) return;
        try {
            String raw = JsonConverter.toJson(resp.getData());
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> wrapper = new Gson().fromJson(raw, mapType);
            if (wrapper == null) return;

            List<Map<String, Object>> items = new ArrayList<>();
            if (wrapper.get("items") instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> mm) items.add((Map<String, Object>) mm);
                }
            }
            if (items.isEmpty()) return;

            // Sort oldest -> newest by bidTime so the chart reads left to right.
            items.sort((a, b) -> Long.compare(readBidTime(a.get("bidTime")),
                                              readBidTime(b.get("bidTime"))));

            // Rebuild chart + history list from scratch.
            series.getData().clear();
            historyData.clear();
            for (Map<String, Object> m : items) {
                long amount = readLong(m.get("bidAmount"));
                String bidder = readStr(m.get("bidderAccountname"));
                long ts = readBidTime(m.get("bidTime"));
                addHistoricChartPoint(amount, ts);
                String when = ts > 0 ? displayFmt.format(new Date(ts)) : "";
                historyData.add(String.format("[%s] %s đặt: %s",
                        when, bidder == null ? "?" : bidder, formatPrice(amount)));
            }
            // Newest first in the list view.
            java.util.Collections.reverse(historyData);
        } catch (Exception ignored) {
            // History is best-effort; failing to draw it shouldn't break the room.
        }
    }

    /** Bid time may arrive as epoch millis (number) or a "yyyy-MM-dd HH:mm:ss"
     *  string depending on serialization — handle both. */
    private long readBidTime(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        String s = o.toString().trim();
        if (s.isEmpty()) return 0L;
        try { return Long.parseLong(s); } catch (Exception ignored) {}
        try { return timestampInFmt.parse(s).getTime(); } catch (Exception ignored) {}
        return 0L;
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
        // Tell the server to stop pushing updates for this room to us.
        if (auctionId > 0) {
            try {
                client.send(new Request(MessageType.LEAVE_AUCTION_ROOM,
                        new org.example.dto.request.AuctionRoomRequest(auctionId)));
            } catch (Exception ignored) { /* best-effort on the way out */ }
        }
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
