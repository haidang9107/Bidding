package org.example.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import org.example.client.dialog.ProfileDialog;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.notification.NotificationService;
import org.example.client.session.Session;
import org.example.client.util.SceneRouter;
import org.example.client.watchlist.MyBidsManager;
import org.example.model.enums.MessageType;
import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for UserDashboard.fxml.
 *
 * Changes (per BTL requirements):
 *  - Renamed "Bán hàng" navigation tile to "Sản phẩm của tôi" (in the FXML).
 *  - Avatar button next to "Đăng xuất" opens {@link ProfileDialog}.
 *  - Subscribes to {@link MyBidsManager} so when a tracked auction ends with
 *    the current user as the winner, a success toast is shown via
 *    {@link NotificationService}.
 *
 * Bug fixes:
 *  - The original code called {@code User#getUsername() / getUserId()}, which
 *    do not exist on {@link User}. Updated to {@code getAccountname()} and
 *    cast to {@link Member} for balance accessors.
 */
public class UserDashboardController {

    // === Wallet ===
    @FXML private Label userLabel;
    @FXML private Label balanceLabel;
    @FXML private Label blockedLabel;

    // === Top-up ===
    @FXML private TextField topUpField;
    @FXML private Button topUpBtn;

    // === Transfer ===
    @FXML private TextField transferTargetField;
    @FXML private TextField transferAmountField;
    @FXML private TextField transferNoteField;
    @FXML private Button transferBtn;

    // === Withdraw ===
    @FXML private TextField withdrawField;
    @FXML private Button withdrawBtn;

    @FXML private Label statusLabel;
    @FXML private ListView<String> historyList;

    @FXML private Button refreshBtn;
    @FXML private Button logoutBtn;
    @FXML private Button avatarBtn;

    private final SocketClient client = SocketClient.getInstance();
    private ServerListener listener;
    private java.util.function.Consumer<Map<Integer, MyBidsManager.Entry>> mybidsSub;
    private java.util.function.Consumer<NotificationService.Notification> notifSub;
    private final ObservableList<String> historyData = FXCollections.observableArrayList();
    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

    // Tracks which auctions we've already notified to avoid duplicate toasts
    private final java.util.Set<Integer> wonNotifiedIds = new java.util.HashSet<>();

    @FXML
    public void initialize() {
        historyList.setItems(historyData);
        listener = this::handleResponse;
        client.addListener(listener);

        // Win notification subscription
        mybidsSub = entries -> Platform.runLater(() -> checkWinNotifications(entries));
        MyBidsManager.getInstance().subscribe(mybidsSub);

        // Mirror outbid/win notifications in the history pane so the user sees them
        // even if they missed the toast
        notifSub = n -> Platform.runLater(() -> addHistory("[" + n.kind + "] " + n.title));
        NotificationService.getInstance().subscribe(notifSub);

        refreshUserUI();
    }

    private void checkWinNotifications(Map<Integer, MyBidsManager.Entry> entries) {
        for (MyBidsManager.Entry e : entries.values()) {
            if (e.status == MyBidsManager.Status.WON && !wonNotifiedIds.contains(e.productId)) {
                wonNotifiedIds.add(e.productId);
                addHistory("Thắng phiên #" + e.productId
                        + " với giá " + formatPrice(e.currentPrice));
            }
        }
    }

    // ============================================================
    // Top-Up
    // ============================================================
    @FXML
    private void handleTopUp() {
        long amount = parseAmount(topUpField);
        if (amount <= 0) {
            statusLabel.setText("Số tiền nạp không hợp lệ!");
            return;
        }
        applyTopUpLocally(amount);
        sendWalletAction("TOP_UP", Map.of("amount", amount));
        topUpField.clear();
    }

    @FXML private void handleTopUpQuick100k() { quickTopUp(100_000L); }
    @FXML private void handleTopUpQuick500k() { quickTopUp(500_000L); }
    @FXML private void handleTopUpQuick1m()   { quickTopUp(1_000_000L); }

    private void quickTopUp(long amount) {
        applyTopUpLocally(amount);
        sendWalletAction("TOP_UP", Map.of("amount", amount));
    }

    private void applyTopUpLocally(long amount) {
        User u = Session.getInstance().getCurrentUser();
        if (!(u instanceof Member m)) return;
        m.setBalance(m.getBalance() + amount);
        addHistory("Nạp " + formatPrice(amount) + " thành công");
        refreshUserUI();
    }

    // ============================================================
    // Transfer
    // ============================================================
    @FXML
    private void handleTransfer() {
        String target = safe(transferTargetField.getText());
        long amount = parseAmount(transferAmountField);
        String note = safe(transferNoteField.getText());

        if (target.isEmpty()) {
            statusLabel.setText("Hãy nhập username người nhận!");
            return;
        }
        User u = Session.getInstance().getCurrentUser();
        if (u == null) return;
        if (target.equalsIgnoreCase(u.getAccountname())) {
            statusLabel.setText("Không thể chuyển cho chính mình!");
            return;
        }
        if (amount <= 0) {
            statusLabel.setText("Số tiền chuyển không hợp lệ!");
            return;
        }
        if (!(u instanceof Member m)) {
            statusLabel.setText("Tài khoản không hỗ trợ chuyển tiền!");
            return;
        }
        long available = m.getBalance() - m.getBlockedBalance();
        if (amount > available) {
            statusLabel.setText("Số dư khả dụng không đủ. Còn lại: " + formatPrice(available));
            return;
        }

        m.setBalance(m.getBalance() - amount);
        addHistory("Chuyển " + formatPrice(amount) + " → " + target
                + (note.isEmpty() ? "" : " (" + note + ")"));
        refreshUserUI();

        Map<String, Object> payload = new HashMap<>();
        payload.put("toAccountname", target);
        payload.put("amount", amount);
        payload.put("note", note);
        sendWalletAction("TRANSFER", payload);

        transferAmountField.clear();
        transferNoteField.clear();
    }

    // ============================================================
    // Withdraw
    // ============================================================
    @FXML
    private void handleWithdraw() {
        long amount = parseAmount(withdrawField);
        if (amount <= 0) {
            statusLabel.setText("Số tiền rút không hợp lệ!");
            return;
        }
        User u = Session.getInstance().getCurrentUser();
        if (!(u instanceof Member m)) return;
        long available = m.getBalance() - m.getBlockedBalance();
        if (amount > available) {
            statusLabel.setText("Số dư khả dụng không đủ. Còn lại: " + formatPrice(available));
            return;
        }
        m.setBalance(m.getBalance() - amount);
        addHistory("Rút " + formatPrice(amount));
        refreshUserUI();
        sendWalletAction("WITHDRAW", Map.of("amount", amount));
        withdrawField.clear();
    }

    // ============================================================
    // Navigation
    // ============================================================
    @FXML
    private void handleGoAuctionList() {
        cleanup();
        SceneRouter.go("/view/AuctionList.fxml", "Sàn đấu giá trực tuyến");
    }

    @FXML
    private void handleGoAuctionList(MouseEvent ev) {
        handleGoAuctionList();
    }

    @FXML
    private void handleGoSellerDashboard() {
        cleanup();
        SceneRouter.go("/view/SellerDashboard.fxml", "Sản phẩm của tôi");
    }

    @FXML
    private void handleGoSellerDashboard(MouseEvent ev) {
        handleGoSellerDashboard();
    }

    @FXML
    private void handleOpenProfile() {
        ProfileDialog.show();
        refreshUserUI();
        User u = Session.getInstance().getCurrentUser();
        if (u != null && avatarBtn != null) {
            avatarBtn.setText(initials(u));
        }
    }

    @FXML
    private void handleRefresh() {
        client.send(new Request(MessageType.GET_PROFILE, null));
        refreshUserUI();
        statusLabel.setText("Đã làm mới.");
    }

    @FXML
    private void handleLogout() {
        client.send(new Request(MessageType.LOGOUT, null));
        Session.getInstance().logout();
        cleanup();
        SceneRouter.go("/view/Login.fxml", "Đăng nhập");
    }

    // ============================================================
    // Network
    // ============================================================
    private void sendWalletAction(String action, Map<String, Object> data) {
        User u = Session.getInstance().getCurrentUser();
        if (u == null) return;
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountname", u.getAccountname());
        payload.put("action", action);
        payload.putAll(data);
        client.send(new Request(MessageType.UPDATE_PROFILE, payload));
    }

    private void handleResponse(Response resp) {
        MessageType t = resp.getType();
        if (t == null) return;

        if (t == MessageType.UPDATE_PROFILE || t == MessageType.GET_PROFILE) {
            Platform.runLater(() -> {
                if (resp.isSuccess() && resp.getData() != null) {
                    try {
                        String raw = JsonConverter.toJson(resp.getData());
                        // Best-effort: server may return a Member; if not parseable just skip.
                        Member m = JsonConverter.fromJson(raw, Member.class);
                        if (m != null && m.getAccountname() != null) {
                            Session.getInstance().setCurrentUser(m);
                            refreshUserUI();
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (resp.getMessage() != null) {
                    statusLabel.setText(resp.getMessage());
                }
            });
        }
    }

    // ============================================================
    // UI helpers
    // ============================================================
    private void refreshUserUI() {
        User u = Session.getInstance().getCurrentUser();
        if (u == null) {
            userLabel.setText("(Chưa đăng nhập)");
            balanceLabel.setText("0 đ");
            blockedLabel.setText("0 đ");
            if (avatarBtn != null) avatarBtn.setText("?");
            return;
        }
        userLabel.setText("Xin chào, " + u.getAccountname() + " (" + u.getRole() + ")");
        if (avatarBtn != null) avatarBtn.setText(initials(u));
        if (u instanceof Member m) {
            long available = m.getBalance() - m.getBlockedBalance();
            balanceLabel.setText(formatPrice(available));
            blockedLabel.setText(formatPrice(m.getBlockedBalance()));
        } else {
            balanceLabel.setText("—");
            blockedLabel.setText("—");
        }
    }

    private void addHistory(String text) {
        historyData.add(0, "[" + fmt.format(new Date()) + "] " + text);
    }

    private long parseAmount(TextField f) {
        try { return Long.parseLong(f.getText().trim()); }
        catch (Exception e) { return -1L; }
    }

    private String formatPrice(long p) { return String.format("%,d đ", p); }
    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static String initials(User u) {
        String src = (u.getFullname() != null && !u.getFullname().isEmpty())
                ? u.getFullname() : u.getAccountname();
        if (src == null || src.isEmpty()) return "?";
        String[] parts = src.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 2; i++) {
            if (!parts[i].isEmpty()) sb.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    private void cleanup() {
        client.removeListener(listener);
        if (mybidsSub != null) MyBidsManager.getInstance().unsubscribe(mybidsSub);
        if (notifSub != null) NotificationService.getInstance().unsubscribe(notifSub);
    }
}
