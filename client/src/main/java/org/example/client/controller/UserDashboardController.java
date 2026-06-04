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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
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
    // Top-Up (Deposit)
    // ============================================================
    @FXML
    private void handleTopUp() {
        long amount = parseAmount(topUpField);
        if (amount <= 0) {
            statusLabel.setText("Số tiền nạp không hợp lệ!");
            return;
        }
        // Server expects MessageType.DEPOSIT with a Long payload (the amount).
        // Server runs FinanceController.handleDeposit which updates the
        // balance row and inserts a transactions row in one commit; the
        // response then carries a BalanceResponse with the new balance.
        statusLabel.setText("Đang gửi yêu cầu nạp " + formatPrice(amount) + "...");
        client.send(new Request(MessageType.DEPOSIT, amount));
        topUpField.clear();
    }

    @FXML private void handleTopUpQuick100k() { prefillTopUp(100_000L); }
    @FXML private void handleTopUpQuick500k() { prefillTopUp(500_000L); }
    @FXML private void handleTopUpQuick1m()   { prefillTopUp(1_000_000L); }

    /**
     * Quick top-up shortcut: just fill the amount into the input field so the
     * user can review (and edit) before they actually press "Nạp tiền". Do
     * NOT send to server here — that only happens in {@link #handleTopUp()}.
     */
    private void prefillTopUp(long amount) {
        topUpField.setText(String.valueOf(amount));
        statusLabel.setText("Đã điền " + formatPrice(amount)
                + " — bấm Nạp tiền để xác nhận.");
    }

    // ============================================================
    // Transfer
    // ============================================================
    @FXML
    private void handleTransfer() {
        String target = safe(transferTargetField.getText());
        long amount = parseAmount(transferAmountField);

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

        // Server's TransferCommand expects a TransferRequest{toAccountname,
        // amount} DTO. The transaction updates both wallets and writes a
        // transactions row in one commit. Don't touch local state — wait
        // for the SUCCESS response then re-fetch the profile.
        org.example.dto.request.TransferRequest req =
                new org.example.dto.request.TransferRequest(target, amount);
        statusLabel.setText("Đang chuyển " + formatPrice(amount) + " → " + target + "...");
        client.send(new Request(MessageType.TRANSFER, req));

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
        // Server's WithdrawCommand expects a Long payload (amount).
        statusLabel.setText("Đang gửi yêu cầu rút " + formatPrice(amount) + "...");
        client.send(new Request(MessageType.WITHDRAW, amount));
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
    private void handleGoSellerDashboard() {
        cleanup();
        // "Sản phẩm của tôi" / "Mở quản lý sản phẩm" navigates to the
        // MyProducts listing screen (inventory + open auctions). The
        // SellerDashboard screen is specifically the "add new product"
        // form and is reached from MyProducts via its "Thêm sản phẩm" button.
        SceneRouter.go("/view/MyProducts.fxml", "Sản phẩm của tôi");
    }

    @FXML
    private void handleOpenNotifications() {
        org.example.client.dialog.NotificationCenterDialog.show();
    }

    @FXML
    private void handleOpenProfile() {
        ProfileDialog.show();
        refreshUserUI();
        // refreshUserUI() already calls applyAvatar(), so the avatar is in
        // sync with whatever was just saved in the dialog.
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
    // (Removed sendWalletAction(): it gửi tất cả wallet action qua
    //  MessageType.UPDATE_PROFILE với field "action" trong payload —
    //  server không hề đọc field đó nên DB không thay đổi. Giờ mỗi handler
    //  gửi đúng endpoint riêng.)

    private void handleResponse(Response resp) {
        MessageType t = resp.getType();
        if (t == null) return;

        if (t == MessageType.UPDATE_PROFILE || t == MessageType.GET_PROFILE) {
            Platform.runLater(() -> {
                if (resp.isSuccess() && resp.getData() != null) {
                    try {
                        String raw = JsonConverter.toJson(resp.getData());
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
            return;
        }

        // Realtime balance push — server emits BALANCE_UPDATE via
        // sendToUser() whenever this account's balance changes for ANY
        // reason (own deposit/withdraw, OR receiving a transfer from someone
        // else). data = BalanceResponse. This is how the receiver of a
        // transfer sees their balance go up without refreshing.
        if (t == MessageType.BALANCE_UPDATE) {
            Platform.runLater(() -> {
                boolean applied = applyBalanceResponse(resp.getData());
                if (applied) {
                    NotificationService.getInstance().info(
                            "Số dư cập nhật",
                            "Số dư của bạn vừa thay đổi.");
                }
            });
            return;
        }

        // Generic server NOTIFICATION (outbid alert, transfer received, etc.)
        if (t == MessageType.NOTIFICATION) {
            Platform.runLater(() -> {
                String body = resp.getMessage() == null ? "Bạn có thông báo mới." : resp.getMessage();
                NotificationService.getInstance().info("Thông báo", body);
            });
            return;
        }

        // Wallet endpoints — server's FinanceController returns Response with
        // the same MessageType as the request (DEPOSIT / WITHDRAW / TRANSFER),
        // NOT MessageType.SUCCESS. The data field carries a BalanceResponse
        // with the new balance, so we can update the UI immediately.
        if (t == MessageType.DEPOSIT || t == MessageType.WITHDRAW
                || t == MessageType.TRANSFER) {
            Platform.runLater(() -> {
                String action = (t == MessageType.DEPOSIT) ? "Nạp tiền"
                             : (t == MessageType.WITHDRAW) ? "Rút tiền"
                             : "Chuyển tiền";
                if (resp.isSuccess()) {
                    String body = resp.getMessage() == null
                            ? action + " thành công." : resp.getMessage();
                    // Toast top-right instead of a flashing status line.
                    NotificationService.getInstance().info("✓ " + action, body);
                    addHistory("✓ " + action + " — " + body);
                    statusLabel.setText("");
                    boolean applied = applyBalanceResponse(resp.getData());
                    if (!applied) {
                        client.send(new Request(MessageType.GET_PROFILE, null));
                    }
                } else {
                    String body = resp.getMessage() == null
                            ? action + " thất bại." : resp.getMessage();
                    NotificationService.getInstance().error("✗ " + action, body);
                    statusLabel.setText("");
                }
            });
            return;
        }

        // Generic SUCCESS / ERROR (e.g. from other endpoints we don't have
        // a specific handler for yet). Show the message so the user gets
        // feedback.
        if (t == MessageType.SUCCESS || t == MessageType.ERROR) {
            Platform.runLater(() -> {
                if (resp.getMessage() != null) {
                    statusLabel.setText((resp.isSuccess() ? "✓ " : "✗ ")
                            + resp.getMessage());
                }
            });
        }
    }

    /**
     * Server returns a BalanceResponse {@code {accountname, newBalance,
     * blockedBalance}} after every wallet operation. Pull the new amounts
     * out and patch the Session's Member so the UI updates immediately.
     *
     * <p>We parse via a generic Map first instead of going straight to
     * BalanceResponse.class — that way custom Gson adapters configured in
     * JsonConverter (like the Product-hierarchy factory) can't accidentally
     * swallow the payload.</p>
     *
     * @return true if the response was successfully applied
     */
    private boolean applyBalanceResponse(Object data) {
        if (data == null) return false;
        try {
            String raw = JsonConverter.toJson(data);
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> m = new Gson().fromJson(raw, mapType);
            if (m == null) return false;

            Long newBalance = readLong(m.get("newBalance"));
            Long blocked    = readLong(m.get("blockedBalance"));
            if (newBalance == null) return false;

            User u = Session.getInstance().getCurrentUser();
            if (u instanceof Member member) {
                member.setBalance(newBalance);
                if (blocked != null) member.setBlockedBalance(blocked);
                refreshUserUI();
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /** Lenient Long reader — Gson decodes numbers as Double when target type
     *  is Object, so we handle both. */
    private static Long readLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); }
        catch (Exception e) { return null; }
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
            if (avatarBtn != null) {
                avatarBtn.setText("?");
                avatarBtn.setGraphic(null);
            }
            return;
        }
        userLabel.setText("Xin chào, " + u.getAccountname() + " (" + u.getRole() + ")");
        applyAvatar(u);
        if (u instanceof Member m) {
            long available = m.getBalance() - m.getBlockedBalance();
            balanceLabel.setText(formatPrice(available));
            blockedLabel.setText(formatPrice(m.getBlockedBalance()));
        } else {
            balanceLabel.setText("—");
            blockedLabel.setText("—");
        }
    }

    /**
     * Renders the user's avatar image inside the top-right round button.
     * Falls back to initials (e.g. "HN") when there's no avatar URL or the
     * image fails to load — so the button is never empty.
     */
    private void applyAvatar(User u) {
        if (avatarBtn == null) return;
        String url = u.getAvt();
        if (url == null || url.isBlank()) {
            avatarBtn.setText(initials(u));
            avatarBtn.setGraphic(null);
            return;
        }
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(
                    url, 36, 36, true, true, true);
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(36);
            iv.setFitHeight(36);
            iv.setPreserveRatio(true);
            // Round-clip so the square image looks like a circular avatar.
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
            iv.setClip(clip);
            avatarBtn.setText("");
            avatarBtn.setGraphic(iv);
            // If the URL turns out to be broken, fall back to initials.
            img.errorProperty().addListener((obs, was, isErr) -> {
                if (Boolean.TRUE.equals(isErr)) {
                    avatarBtn.setGraphic(null);
                    avatarBtn.setText(initials(u));
                }
            });
        } catch (Exception ignored) {
            avatarBtn.setText(initials(u));
            avatarBtn.setGraphic(null);
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
