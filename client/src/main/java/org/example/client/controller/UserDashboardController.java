package org.example.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.session.Session;
import org.example.client.util.SceneRouter;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller cho UserDashboard.fxml.
 *
 * Chức năng:
 *  - Hiển thị số dư khả dụng / số dư đang khóa của user.
 *  - Nạp tiền (top-up).
 *  - Chuyển tiền cho user khác.
 *  - Rút tiền.
 *  - Lịch sử giao dịch trong phiên hiện tại.
 *  - Điều hướng sang AuctionList (xem chi tiết phiên) hoặc SellerDashboard.
 *
 * Lưu ý kỹ thuật:
 *  Hệ thống hiện tại chưa định nghĩa MessageType riêng cho ví. Để KHÔNG đổi
 *  logic / cấu trúc enum đã có, controller này dùng MessageType.UPDATE_PROFILE
 *  với payload là JSON-Map chứa "action" (TOP_UP / TRANSFER / WITHDRAW) cùng
 *  số liệu. Server hiện trả về "Profile features are coming soon!"; khi server
 *  được mở rộng, payload đã sẵn sàng để xử lý mà không cần đổi client.
 *
 *  Trong khi chờ server hỗ trợ, controller cũng cập nhật trực tiếp lên đối tượng
 *  User trong Session để người dùng có trải nghiệm liên tục ngay trong phiên
 *  đang chạy (số dư hiển thị được điều chỉnh phía client, vẫn gửi request lên
 *  server để khi server bổ sung là đồng bộ).
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

    private final SocketClient client = SocketClient.getInstance();
    private ServerListener listener;
    private final ObservableList<String> historyData = FXCollections.observableArrayList();
    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

    @FXML
    public void initialize() {
        historyList.setItems(historyData);

        listener = this::handleResponse;
        client.addListener(listener);

        refreshUserUI();
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

    @FXML
    private void handleTopUpQuick100k() {
        quickTopUp(100_000L);
    }

    @FXML
    private void handleTopUpQuick500k() {
        quickTopUp(500_000L);
    }

    @FXML
    private void handleTopUpQuick1m() {
        quickTopUp(1_000_000L);
    }

    private void quickTopUp(long amount) {
        applyTopUpLocally(amount);
        sendWalletAction("TOP_UP", Map.of("amount", amount));
    }

    private void applyTopUpLocally(long amount) {
        User u = Session.getInstance().getCurrentUser();
        if (u == null) return;
        u.setBalance(u.getBalance() + amount);
        addHistory("Nạp " + formatPrice(amount) + " thành công");
        refreshUserUI();
    }

    // ============================================================
    // Transfer
    // ============================================================
    @FXML
    private void handleTransfer() {
        String target = transferTargetField.getText() == null ? "" : transferTargetField.getText().trim();
        long amount = parseAmount(transferAmountField);
        String note = transferNoteField.getText() == null ? "" : transferNoteField.getText().trim();

        if (target.isEmpty()) {
            statusLabel.setText("Hãy nhập username người nhận!");
            return;
        }
        User u = Session.getInstance().getCurrentUser();
        if (u == null) return;
        if (target.equalsIgnoreCase(u.getUsername())) {
            statusLabel.setText("Không thể chuyển cho chính mình!");
            return;
        }
        if (amount <= 0) {
            statusLabel.setText("Số tiền chuyển không hợp lệ!");
            return;
        }
        long available = u.getBalance() - u.getBlockedBalance();
        if (amount > available) {
            statusLabel.setText("Số dư khả dụng không đủ. Còn lại: " + formatPrice(available));
            return;
        }

        // Local update để UX mượt
        u.setBalance(u.getBalance() - amount);
        addHistory("Chuyển " + formatPrice(amount) + " → " + target
                + (note.isEmpty() ? "" : " (" + note + ")"));
        refreshUserUI();

        // Gửi request lên server
        Map<String, Object> payload = new HashMap<>();
        payload.put("toUsername", target);
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
        if (u == null) return;
        long available = u.getBalance() - u.getBlockedBalance();
        if (amount > available) {
            statusLabel.setText("Số dư khả dụng không đủ. Còn lại: " + formatPrice(available));
            return;
        }
        u.setBalance(u.getBalance() - amount);
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
        SceneRouter.go("/view/SellerDashboard.fxml", "Bảng điều khiển Người bán");
    }

    @FXML
    private void handleGoSellerDashboard(MouseEvent ev) {
        handleGoSellerDashboard();
    }

    @FXML
    private void handleRefresh() {
        // Yêu cầu server gửi lại profile (server hiện trả "coming soon" – vẫn cập nhật UI từ Session)
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
        payload.put("userId", u.getUserId());
        payload.put("action", action);
        payload.putAll(data);
        client.send(new Request(MessageType.UPDATE_PROFILE, JsonConverter.toJson(payload)));
    }

    private void handleResponse(Response resp) {
        MessageType t = resp.getType();
        if (t == null) return;

        // Khi server bổ sung UPDATE_PROFILE và trả về User mới, ta cập nhật Session
        if (t == MessageType.UPDATE_PROFILE || t == MessageType.GET_PROFILE) {
            Platform.runLater(() -> {
                if (resp.isSuccess() && resp.getData() != null) {
                    // Cố gắng parse thành User; nếu thất bại (data là Map) thì bỏ qua
                    try {
                        String raw = JsonConverter.toJson(resp.getData());
                        User u = JsonConverter.fromJson(raw, User.class);
                        if (u != null) {
                            // User là abstract → Gson không thể tạo, nhưng nếu server trả về Member
                            // thì raw đã chứa role. Bỏ qua nếu null.
                            Session.getInstance().setCurrentUser(u);
                            refreshUserUI();
                        }
                    } catch (Exception ignored) { /* server có thể chưa hỗ trợ */ }
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
            return;
        }
        userLabel.setText("Xin chào, " + u.getUsername() + " (" + u.getRole() + ")");
        long available = u.getBalance() - u.getBlockedBalance();
        balanceLabel.setText(formatPrice(available));
        blockedLabel.setText(formatPrice(u.getBlockedBalance()));
    }

    private void addHistory(String text) {
        historyData.add(0, "[" + fmt.format(new Date()) + "] " + text);
    }

    private long parseAmount(TextField f) {
        try {
            return Long.parseLong(f.getText().trim());
        } catch (Exception e) {
            return -1L;
        }
    }

    private String formatPrice(long p) {
        return String.format("%,d đ", p);
    }

    private void cleanup() {
        client.removeListener(listener);
    }

    /**
     * Dùng trong trường hợp các màn hình khác muốn show alert.
     */
    @SuppressWarnings("unused")
    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
