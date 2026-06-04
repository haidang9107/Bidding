package org.example.client.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.notification.NotificationService;
import org.example.client.session.Session;
import org.example.client.util.SceneRouter;
import org.example.dto.request.AdminUserControlRequest;
import org.example.dto.request.AuctionCancelRequest;
import org.example.dto.request.PaginationRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Controller for the Admin Dashboard. Four tabs:
 *  - Users: list / detail / ban / unban.
 *  - Auctions: list / detail / cancel.
 *  - Products: derived from auctions, grouped by productId.
 *  - Stats: aggregate counters.
 *
 * <p>Note on "delete user": the server does not expose a hard-delete endpoint
 * (only ban/unban via {@code ADMIN_BAN_USER}). The UI calls it "Khoá tài khoản"
 * to avoid implying data is destroyed.
 */
public class AdminDashboardController {

    // ============== Topbar ==============
    @FXML private Label userLabel;
    @FXML private Button refreshBtn;
    @FXML private Button logoutBtn;
    @FXML private TabPane tabPane;

    // ============== Users tab ==============
    @FXML private TextField userSearchField;
    @FXML private ChoiceBox<String> userRoleFilter;
    @FXML private ChoiceBox<String> userStatusFilter;
    @FXML private Label userSummaryLabel;
    @FXML private TableView<UserRow> userTable;
    @FXML private TableColumn<UserRow, String> colAcc;
    @FXML private TableColumn<UserRow, String> colName;
    @FXML private TableColumn<UserRow, String> colEmail;
    @FXML private TableColumn<UserRow, String> colRole;
    @FXML private TableColumn<UserRow, String> colStatus;
    @FXML private TableColumn<UserRow, String> colBalance;
    @FXML private TableColumn<UserRow, String> colBlocked;
    @FXML private Button userDetailBtn;
    @FXML private Button userBanBtn;
    @FXML private Button userUnbanBtn;

    // ============== Auctions tab ==============
    @FXML private TextField auctionSearchField;
    @FXML private ChoiceBox<String> auctionStatusFilter;
    @FXML private Label auctionSummaryLabel;
    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, String> colAuctionId;
    @FXML private TableColumn<AuctionRow, String> colAucProduct;
    @FXML private TableColumn<AuctionRow, String> colAucSeller;
    @FXML private TableColumn<AuctionRow, String> colAucWinner;
    @FXML private TableColumn<AuctionRow, String> colAucPrice;
    @FXML private TableColumn<AuctionRow, String> colAucStatus;
    @FXML private TableColumn<AuctionRow, String> colAucEnd;

    // ============== Products tab ==============
    @FXML private TextField productSearchField;
    @FXML private ChoiceBox<String> productCategoryFilter;
    @FXML private Label productSummaryLabel;
    @FXML private TableView<ProductRow> productTable;
    @FXML private TableColumn<ProductRow, String> colPid;
    @FXML private TableColumn<ProductRow, String> colPName;
    @FXML private TableColumn<ProductRow, String> colPCat;
    @FXML private TableColumn<ProductRow, String> colPOwner;
    @FXML private TableColumn<ProductRow, String> colPInAuc;
    @FXML private TableColumn<ProductRow, String> colPSessions;

    // ============== Stats tab ==============
    @FXML private Label statTotalUsers;
    @FXML private Label statBannedUsers;
    @FXML private Label statRunningAuctions;
    @FXML private Label statFinishedAuctions;
    @FXML private Label statCanceledAuctions;
    @FXML private Label statTotalProducts;
    @FXML private Label statTotalLocked;
    @FXML private Label statTotalBalance;
    @FXML private Label statHint;

    // ============== State ==============
    private final SocketClient client = SocketClient.getInstance();
    private ServerListener listener;
    /** True between sending ADMIN_GET_ALL_USERS and receiving its SUCCESS
     *  reply, so we can tell the user-list response apart from ban/cancel
     *  acks (the server uses a generic SUCCESS envelope for all of them). */
    private boolean awaitingUserList = false;

    // ===== Pagination (Users tab) =====
    @FXML private Label userPageLabel;
    @FXML private Button userFirstBtn;
    @FXML private Button userPrevBtn;
    @FXML private Button userNextBtn;
    @FXML private Button userLastBtn;
    private int userPage = 1;
    private int userTotalPages = 1;

    // ===== Pagination (Auctions tab) =====
    @FXML private Label aucPageLabel;
    @FXML private Button aucFirstBtn;
    @FXML private Button aucPrevBtn;
    @FXML private Button aucNextBtn;
    @FXML private Button aucLastBtn;
    private int aucPage = 1;
    private int aucTotalPages = 1;

    private static final int ADMIN_PAGE_SIZE = 20;

    private final ObservableList<UserRow> usersData = FXCollections.observableArrayList();
    private final ObservableList<AuctionRow> auctionsData = FXCollections.observableArrayList();
    private final ObservableList<ProductRow> productsData = FXCollections.observableArrayList();
    private final List<UserRow> allUsers = new ArrayList<>();
    private final List<AuctionRow> allAuctions = new ArrayList<>();
    private final List<ProductRow> allProducts = new ArrayList<>();

    private final SimpleDateFormat displayFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private static final String ROLE_ALL = "Tất cả", ROLE_ADMIN = "ADMIN", ROLE_MEMBER = "MEMBER";
    private static final String STATUS_ALL = "Tất cả", STATUS_ACTIVE = "Đang hoạt động", STATUS_BANNED = "Đã khoá";
    private static final String AUC_ALL = "Tất cả", AUC_OPEN = "OPEN", AUC_RUNNING = "RUNNING",
                                AUC_FINISHED = "FINISHED", AUC_CANCELED = "CANCELED";
    private static final String CAT_ALL = "Tất cả", CAT_ELECTRONICS = "ELECTRONICS",
                                CAT_ART = "ART", CAT_VEHICLE = "VEHICLE", CAT_OTHER = "OTHER";

    @FXML
    public void initialize() {
        User u = Session.getInstance().getCurrentUser();
        if (u != null) {
            userLabel.setText("Quản trị viên: " + u.getAccountname());
        }

        setupUserTab();
        setupAuctionTab();
        setupProductTab();

        listener = this::handleResponse;
        client.addListener(listener);

        // Initial load
        loadAll();
    }

    // ============================================================
    // Setup tables
    // ============================================================
    private void setupUserTab() {
        userRoleFilter.setItems(FXCollections.observableArrayList(ROLE_ALL, ROLE_ADMIN, ROLE_MEMBER));
        userRoleFilter.setValue(ROLE_ALL);
        userStatusFilter.setItems(FXCollections.observableArrayList(STATUS_ALL, STATUS_ACTIVE, STATUS_BANNED));
        userStatusFilter.setValue(STATUS_ALL);

        colAcc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().accountname));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fullname));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().email));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().role));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().status == 0 ? "Hoạt động" : "Đã khoá"));
        colBalance.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().balance == null ? "—" : formatPrice(c.getValue().balance)));
        colBlocked.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().blockedBalance == null ? "—" : formatPrice(c.getValue().blockedBalance)));

        userTable.setItems(usersData);

        userSearchField.textProperty().addListener((o, ov, nv) -> applyUserFilters());
        userRoleFilter.valueProperty().addListener((o, ov, nv) -> applyUserFilters());
        userStatusFilter.valueProperty().addListener((o, ov, nv) -> applyUserFilters());
    }

    private void setupAuctionTab() {
        auctionStatusFilter.setItems(FXCollections.observableArrayList(
                AUC_ALL, AUC_OPEN, AUC_RUNNING, AUC_FINISHED, AUC_CANCELED));
        auctionStatusFilter.setValue(AUC_ALL);

        colAuctionId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().auctionId)));
        colAucProduct.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().productName));
        colAucSeller.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().sellerAccountname));
        colAucWinner.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().winnerAccountname == null ? "(chưa có)" : c.getValue().winnerAccountname));
        colAucPrice.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().currentPrice)));
        colAucStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));
        colAucEnd.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().endTime));

        auctionTable.setItems(auctionsData);

        auctionSearchField.textProperty().addListener((o, ov, nv) -> applyAuctionFilters());
        auctionStatusFilter.valueProperty().addListener((o, ov, nv) -> applyAuctionFilters());
    }

    private void setupProductTab() {
        productCategoryFilter.setItems(FXCollections.observableArrayList(
                CAT_ALL, CAT_ELECTRONICS, CAT_ART, CAT_VEHICLE, CAT_OTHER));
        productCategoryFilter.setValue(CAT_ALL);

        colPid.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().productId)));
        colPName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name));
        colPCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().category));
        colPOwner.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().ownerAccountname == null ? "(không rõ)" : c.getValue().ownerAccountname));
        colPInAuc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().inAuction ? "Đang đấu" : "Không"));
        colPSessions.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().sessions)));

        productTable.setItems(productsData);

        productSearchField.textProperty().addListener((o, ov, nv) -> applyProductFilters());
        productCategoryFilter.valueProperty().addListener((o, ov, nv) -> applyProductFilters());
    }

    // ============================================================
    // Actions
    // ============================================================
    @FXML
    private void handleRefreshAll() {
        loadAll();
    }

    @FXML
    private void handleLogout() {
        client.send(new Request(MessageType.LOGOUT, null));
        Session.getInstance().logout();
        cleanup();
        SceneRouter.go("/view/Login.fxml", "Đăng nhập");
    }

    @FXML
    private void handleRefreshUsers() { loadUsers(); }

    @FXML
    private void handleRefreshAuctions() { loadAuctions(); }

    @FXML
    private void handleUserDetail() {
        UserRow row = userTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            statusInfo("Hãy chọn một người dùng trong bảng.");
            return;
        }
        showUserDetailDialog(row);
    }

    @FXML
    private void handleBanUser() {
        UserRow row = userTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            statusInfo("Hãy chọn một người dùng để khoá.");
            return;
        }
        if (row.status == 1) {
            statusInfo("Tài khoản này đã ở trạng thái khoá.");
            return;
        }
        if (!confirm("Khoá tài khoản",
                "Bạn có chắc muốn khoá tài khoản '" + row.accountname + "'?\n"
                + "Người dùng sẽ không thể đăng nhập cho đến khi được mở khoá.")) return;
        sendBan(row.accountname, 1);
    }

    @FXML
    private void handleUnbanUser() {
        UserRow row = userTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            statusInfo("Hãy chọn một người dùng để mở khoá.");
            return;
        }
        if (row.status == 0) {
            statusInfo("Tài khoản này đang hoạt động bình thường.");
            return;
        }
        sendBan(row.accountname, 0);
    }

    @FXML
    private void handleAuctionDetail() {
        AuctionRow row = auctionTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            statusInfo("Hãy chọn một phiên đấu giá.");
            return;
        }
        showAuctionDetailDialog(row);
    }

    @FXML
    private void handleCancelAuction() {
        AuctionRow row = auctionTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            statusInfo("Hãy chọn một phiên để huỷ.");
            return;
        }
        if ("FINISHED".equalsIgnoreCase(row.status) || "CANCELED".equalsIgnoreCase(row.status)) {
            statusInfo("Phiên này đã kết thúc, không thể huỷ.");
            return;
        }
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Huỷ phiên đấu giá");
        dlg.setHeaderText("Phiên #" + row.auctionId + " — " + row.productName);
        dlg.setContentText("Lý do huỷ (tuỳ chọn):");
        Optional<String> result = dlg.showAndWait();
        if (result.isEmpty()) return;
        sendCancelAuction(row.auctionId, result.get());
    }

    // ============================================================
    // Network — sending
    // ============================================================
    private void loadAll() {
        loadUsers();
        loadAuctions();
    }

    private void loadUsers() {
        loadUserPage(userPage);
    }

    /** Request one page of users. Server caps results server-side; paging
     *  through lets the admin reach ALL users, not just the first window. */
    private void loadUserPage(int page) {
        if (page < 1) page = 1;
        userPage = page;
        awaitingUserList = true;
        client.send(new Request(MessageType.ADMIN_GET_ALL_USERS,
                new PaginationRequest(page, ADMIN_PAGE_SIZE)));
        userSummaryLabel.setText("Đang tải...");
    }

    @FXML private void handleUserFirstPage() { if (userPage != 1) loadUserPage(1); }
    @FXML private void handleUserPrevPage()  { if (userPage > 1) loadUserPage(userPage - 1); }
    @FXML private void handleUserNextPage()  { if (userPage < userTotalPages) loadUserPage(userPage + 1); }
    @FXML private void handleUserLastPage()  { if (userPage != userTotalPages) loadUserPage(userTotalPages); }

    private void updateUserPageControls() {
        if (userPageLabel != null) userPageLabel.setText("Trang " + userPage + "/" + Math.max(1, userTotalPages));
        if (userFirstBtn != null) userFirstBtn.setDisable(userPage <= 1);
        if (userPrevBtn  != null) userPrevBtn.setDisable(userPage <= 1);
        if (userNextBtn  != null) userNextBtn.setDisable(userPage >= userTotalPages);
        if (userLastBtn  != null) userLastBtn.setDisable(userPage >= userTotalPages);
    }

    private void loadAuctions() {
        loadAuctionPage(aucPage);
    }

    private void loadAuctionPage(int page) {
        if (page < 1) page = 1;
        aucPage = page;
        client.send(new Request(MessageType.PRODUCT_LIST,
                new PaginationRequest(page, ADMIN_PAGE_SIZE)));
        auctionSummaryLabel.setText("Đang tải...");
        productSummaryLabel.setText("Đang tải...");
    }

    @FXML private void handleAucFirstPage() { if (aucPage != 1) loadAuctionPage(1); }
    @FXML private void handleAucPrevPage()  { if (aucPage > 1) loadAuctionPage(aucPage - 1); }
    @FXML private void handleAucNextPage()  { if (aucPage < aucTotalPages) loadAuctionPage(aucPage + 1); }
    @FXML private void handleAucLastPage()  { if (aucPage != aucTotalPages) loadAuctionPage(aucTotalPages); }

    private void updateAucPageControls() {
        if (aucPageLabel != null) aucPageLabel.setText("Trang " + aucPage + "/" + Math.max(1, aucTotalPages));
        if (aucFirstBtn != null) aucFirstBtn.setDisable(aucPage <= 1);
        if (aucPrevBtn  != null) aucPrevBtn.setDisable(aucPage <= 1);
        if (aucNextBtn  != null) aucNextBtn.setDisable(aucPage >= aucTotalPages);
        if (aucLastBtn  != null) aucLastBtn.setDisable(aucPage >= aucTotalPages);
    }

    private void sendBan(String accountname, int newStatus) {
        AdminUserControlRequest req = new AdminUserControlRequest(accountname, newStatus);
        client.send(new Request(MessageType.ADMIN_BAN_USER, req));
    }

    private void sendCancelAuction(int auctionId, String reason) {
        AuctionCancelRequest req = new AuctionCancelRequest(auctionId, reason);
        client.send(new Request(MessageType.ADMIN_CANCEL_AUCTION, req));
    }

    // ============================================================
    // Network — receiving
    // ============================================================
    private void handleResponse(Response resp) {
        MessageType t = resp.getType();
        if (t == null) return;
        switch (t) {
            // Server returns the user list under a generic SUCCESS envelope
            // (data = PagedResponse<UserResponse>), so we detect it by the
            // pending flag we set when we sent ADMIN_GET_ALL_USERS.
            case SUCCESS -> Platform.runLater(() -> {
                if (awaitingUserList) {
                    awaitingUserList = false;
                    onUserList(resp);
                } else {
                    onAdminSuccess(resp);
                }
            });
            case ADMIN_GET_ALL_USERS -> Platform.runLater(() -> onUserList(resp));
            case PRODUCT_LIST        -> Platform.runLater(() -> onAuctionList(resp));
            case ERROR               -> Platform.runLater(() -> {
                awaitingUserList = false;
                onAdminError(resp);
            });
            default -> { /* ignore */ }
        }
    }

    @SuppressWarnings("unchecked")
    private void onUserList(Response resp) {
        if (!resp.isSuccess()) {
            userSummaryLabel.setText("Không tải được. " + resp.getMessage());
            return;
        }
        String raw = JsonConverter.toJson(resp.getData());
        List<Map<String, Object>> items = extractItems(raw);
        // Sync pagination from the PagedResponse wrapper.
        int[] pg = extractPaging(raw);
        if (pg[0] > 0) userPage = pg[0];
        userTotalPages = Math.max(1, pg[1]);
        updateUserPageControls();

        allUsers.clear();
        for (Map<String, Object> m : items) {
            allUsers.add(UserRow.fromMap(m));
        }
        applyUserFilters();
        refreshStats();
    }

    @SuppressWarnings("unchecked")
    private void onAuctionList(Response resp) {
        if (!resp.isSuccess()) {
            auctionSummaryLabel.setText("Không tải được. " + resp.getMessage());
            return;
        }
        String raw = JsonConverter.toJson(resp.getData());
        List<Map<String, Object>> items = extractItems(raw);
        int[] apg = extractPaging(raw);
        if (apg[0] > 0) aucPage = apg[0];
        aucTotalPages = Math.max(1, apg[1]);
        updateAucPageControls();

        allAuctions.clear();
        Map<Integer, ProductRow> productsByPid = new HashMap<>();

        for (Map<String, Object> m : items) {
            AuctionRow ar = AuctionRow.fromMap(m, displayFmt);
            allAuctions.add(ar);

            // Aggregate per-product
            ProductRow pr = productsByPid.computeIfAbsent(ar.productId, k -> {
                ProductRow p = new ProductRow();
                p.productId = ar.productId;
                p.name = ar.productName;
                p.category = ar.category;
                p.ownerAccountname = readStr(m.get("ownerAccountname"), null);
                p.inAuction = false;
                p.sessions = 0;
                return p;
            });
            pr.sessions++;
            if ("RUNNING".equalsIgnoreCase(ar.status) || "OPEN".equalsIgnoreCase(ar.status)) {
                pr.inAuction = true;
            }
        }

        allProducts.clear();
        allProducts.addAll(productsByPid.values());

        applyAuctionFilters();
        applyProductFilters();
        refreshStats();
    }

    private void onAdminSuccess(Response resp) {
        NotificationService.getInstance().info("Thao tác thành công",
                resp.getMessage() == null ? "OK" : resp.getMessage());
        // Reload both lists since the action could have affected either
        loadUsers();
        loadAuctions();
    }

    private void onAdminError(Response resp) {
        NotificationService.getInstance().error("Lỗi",
                resp.getMessage() == null ? "Không rõ" : resp.getMessage());
    }

    // ============================================================
    // Filters & rendering
    // ============================================================
    private void applyUserFilters() {
        String search = textLower(userSearchField);
        String role = userRoleFilter.getValue();
        String status = userStatusFilter.getValue();

        usersData.clear();
        for (UserRow r : allUsers) {
            if (!search.isEmpty()
                    && !((r.accountname != null && r.accountname.toLowerCase(Locale.ROOT).contains(search))
                       || (r.email != null && r.email.toLowerCase(Locale.ROOT).contains(search)))) {
                continue;
            }
            if (!ROLE_ALL.equals(role) && !role.equalsIgnoreCase(r.role)) continue;
            if (STATUS_ACTIVE.equals(status) && r.status != 0) continue;
            if (STATUS_BANNED.equals(status) && r.status != 1) continue;
            usersData.add(r);
        }
        userSummaryLabel.setText("Hiển thị " + usersData.size() + " / " + allUsers.size() + " người dùng");
    }

    private void applyAuctionFilters() {
        String search = textLower(auctionSearchField);
        String status = auctionStatusFilter.getValue();

        auctionsData.clear();
        for (AuctionRow r : allAuctions) {
            if (!search.isEmpty()
                    && !((r.productName != null && r.productName.toLowerCase(Locale.ROOT).contains(search))
                       || (r.sellerAccountname != null && r.sellerAccountname.toLowerCase(Locale.ROOT).contains(search)))) {
                continue;
            }
            if (!AUC_ALL.equals(status) && !status.equalsIgnoreCase(r.status)) continue;
            auctionsData.add(r);
        }
        auctionSummaryLabel.setText("Hiển thị " + auctionsData.size() + " / " + allAuctions.size() + " phiên");
    }

    private void applyProductFilters() {
        String search = textLower(productSearchField);
        String cat = productCategoryFilter.getValue();

        productsData.clear();
        for (ProductRow r : allProducts) {
            if (!search.isEmpty()
                    && (r.name == null || !r.name.toLowerCase(Locale.ROOT).contains(search))) {
                continue;
            }
            if (!CAT_ALL.equals(cat) && !cat.equalsIgnoreCase(r.category)) continue;
            productsData.add(r);
        }
        productSummaryLabel.setText("Hiển thị " + productsData.size() + " / " + allProducts.size() + " sản phẩm");
    }

    private void refreshStats() {
        int total = allUsers.size();
        int banned = 0;
        long sumBalance = 0, sumBlocked = 0;
        for (UserRow u : allUsers) {
            if (u.status == 1) banned++;
            if (u.balance != null) sumBalance += u.balance;
            if (u.blockedBalance != null) sumBlocked += u.blockedBalance;
        }
        int running = 0, finished = 0, canceled = 0;
        for (AuctionRow a : allAuctions) {
            switch (a.status == null ? "" : a.status.toUpperCase(Locale.ROOT)) {
                case "RUNNING"  -> running++;
                case "FINISHED", "PAID" -> finished++;
                case "CANCELED" -> canceled++;
                default -> {}
            }
        }
        statTotalUsers.setText(String.valueOf(total));
        statBannedUsers.setText(String.valueOf(banned));
        statRunningAuctions.setText(String.valueOf(running));
        statFinishedAuctions.setText(String.valueOf(finished));
        statCanceledAuctions.setText(String.valueOf(canceled));
        statTotalProducts.setText(String.valueOf(allProducts.size()));
        statTotalLocked.setText(formatPrice(sumBlocked));
        statTotalBalance.setText(formatPrice(sumBalance));
    }

    // ============================================================
    // Dialogs
    // ============================================================
    private void showUserDetailDialog(UserRow r) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Thông tin người dùng");
        a.setHeaderText(r.fullname == null || r.fullname.isEmpty() ? r.accountname : r.fullname);
        StringBuilder sb = new StringBuilder();
        sb.append("Tài khoản: ").append(r.accountname).append("\n");
        sb.append("Họ tên:    ").append(safe(r.fullname)).append("\n");
        sb.append("Email:     ").append(safe(r.email)).append("\n");
        sb.append("Vai trò:   ").append(r.role).append("\n");
        sb.append("Trạng thái: ").append(r.status == 0 ? "Hoạt động" : "Đã khoá").append("\n");
        if (r.balance != null) {
            sb.append("Số dư:        ").append(formatPrice(r.balance)).append("\n");
            sb.append("Đang khoá:    ").append(formatPrice(r.blockedBalance == null ? 0 : r.blockedBalance)).append("\n");
            long available = r.balance - (r.blockedBalance == null ? 0 : r.blockedBalance);
            sb.append("Khả dụng:     ").append(formatPrice(available));
        }
        a.setContentText(sb.toString());
        a.getDialogPane().setMinWidth(440);
        a.showAndWait();
    }

    private void showAuctionDetailDialog(AuctionRow r) {
        ButtonType enterRoom = new ButtonType("Vào phòng đấu giá", ButtonBar.ButtonData.OK_DONE);
        Alert a = new Alert(Alert.AlertType.INFORMATION, "", enterRoom, ButtonType.CLOSE);
        a.setTitle("Chi tiết phiên đấu giá");
        a.setHeaderText("Phiên #" + r.auctionId + " — " + r.productName);
        StringBuilder sb = new StringBuilder();
        sb.append("ID phiên:        ").append(r.auctionId).append("\n");
        sb.append("ID sản phẩm:     ").append(r.productId).append("\n");
        sb.append("Sản phẩm:        ").append(safe(r.productName)).append("\n");
        sb.append("Danh mục:        ").append(safe(r.category)).append("\n");
        sb.append("Người bán:       ").append(safe(r.sellerAccountname)).append("\n");
        sb.append("Người dẫn đầu:   ").append(r.winnerAccountname == null ? "(chưa có)" : r.winnerAccountname).append("\n");
        sb.append("Giá khởi điểm:   ").append(formatPrice(r.startingPrice)).append("\n");
        sb.append("Giá hiện tại:    ").append(formatPrice(r.currentPrice)).append("\n");
        sb.append("Bước giá:        ").append(formatPrice(r.stepPrice)).append("\n");
        sb.append("Trạng thái:      ").append(r.status).append("\n");
        sb.append("Bắt đầu:         ").append(r.startTime).append("\n");
        sb.append("Kết thúc:        ").append(r.endTime);
        a.setContentText(sb.toString());
        a.getDialogPane().setMinWidth(480);

        Optional<ButtonType> choice = a.showAndWait();
        if (choice.isPresent() && choice.get() == enterRoom) {
            // Open the live auction room for this specific auction.
            cleanup();
            final int aId = r.auctionId;
            SceneRouter.go("/view/AuctionDetail.fxml",
                    "Phiên đấu giá #" + aId,
                    (AuctionDetailController c) -> c.setAuctionId(aId));
        }
    }

    private boolean confirm(String title, String body) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, body, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private void statusInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    // ============================================================
    // Helpers
    // ============================================================
    @SuppressWarnings("unchecked")
    /** Reads {currentPage,totalPages} from a PagedResponse JSON; returns
     *  {0,1} when the shape is a plain list (no wrapper). */
    private int[] extractPaging(String raw) {
        try {
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> paged = new Gson().fromJson(raw, mapType);
            if (paged != null && paged.get("items") instanceof List<?>) {
                int cur = readInt(paged.get("currentPage"));
                int tot = readInt(paged.get("totalPages"));
                return new int[]{cur, tot};
            }
        } catch (Exception ignored) {}
        return new int[]{0, 1};
    }

    private List<Map<String, Object>> extractItems(String raw) {
        try {
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> paged = new Gson().fromJson(raw, mapType);
            if (paged != null && paged.get("items") instanceof List<?> list) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof Map<?, ?> mm) out.add((Map<String, Object>) mm);
                }
                return out;
            }
        } catch (Exception ignored) {}
        try {
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> list = new Gson().fromJson(raw, listType);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private void cleanup() {
        client.removeListener(listener);
    }

    private static String safe(String s) { return s == null || s.isEmpty() ? "—" : s; }
    private static String textLower(TextField f) {
        return f == null || f.getText() == null ? "" : f.getText().trim().toLowerCase(Locale.ROOT);
    }
    private static String formatPrice(long p) { return String.format("%,d đ", p); }
    private static int readInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }
    private static long readLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }
    private static Long readLongNullable(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }
    private static String readStr(Object o, String fallback) {
        if (o == null) return fallback;
        String s = o.toString();
        return s.isEmpty() ? fallback : s;
    }

    // ============================================================
    // Row models
    // ============================================================
    public static class UserRow {
        String accountname;
        String fullname;
        String email;
        String role;
        int status;
        Long balance;
        Long blockedBalance;

        static UserRow fromMap(Map<String, Object> m) {
            UserRow r = new UserRow();
            r.accountname = readStr(m.get("accountname"), "");
            r.fullname    = readStr(m.get("fullname"), "");
            r.email       = readStr(m.get("email"), "");
            Object role = m.get("role");
            r.role        = role == null ? "MEMBER" : role.toString();
            r.status      = readInt(m.get("status"));
            r.balance        = readLongNullable(m.get("balance"));
            r.blockedBalance = readLongNullable(m.get("blockedBalance"));
            return r;
        }
    }

    public static class AuctionRow {
        int auctionId;
        int productId;
        String productName;
        String category;
        String sellerAccountname;
        String winnerAccountname;
        long startingPrice;
        long currentPrice;
        long stepPrice;
        String status;
        String startTime;
        String endTime;

        static AuctionRow fromMap(Map<String, Object> m, SimpleDateFormat fmt) {
            AuctionRow r = new AuctionRow();
            r.auctionId = readInt(m.get("auctionId"));
            r.productId = readInt(m.get("productId"));
            r.productName = readStr(m.get("name"), readStr(m.get("productName"), ""));
            r.category = readStr(m.get("category"), "OTHER");
            r.sellerAccountname = readStr(m.get("sellerAccountname"), "");
            r.winnerAccountname = readStr(m.get("winnerAccountname"), null);
            r.startingPrice = readLong(m.get("startingPrice"));
            r.currentPrice = readLong(m.get("currentPrice"));
            r.stepPrice = readLong(m.get("stepPrice"));
            r.status = readStr(m.get("status"), "");
            r.startTime = formatTs(m.get("startTime"), fmt);
            r.endTime = formatTs(m.get("endTime"), fmt);
            return r;
        }

        private static String formatTs(Object o, SimpleDateFormat fmt) {
            if (o == null) return "";
            if (o instanceof String s) {
                try {
                    SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date d = in.parse(s);
                    return fmt.format(d);
                } catch (Exception ex) {
                    return s;
                }
            }
            return o.toString();
        }
    }

    public static class ProductRow {
        int productId;
        String name;
        String category;
        String ownerAccountname;
        boolean inAuction;
        int sessions;
    }
}
