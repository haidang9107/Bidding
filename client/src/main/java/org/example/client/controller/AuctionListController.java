package org.example.client.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.client.component.ProductCard;
import org.example.client.dialog.ProfileDialog;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.notification.NotificationService;
import org.example.client.session.Session;
import org.example.client.util.SceneRouter;
import org.example.client.watchlist.MyBidsManager;
import org.example.client.watchlist.WatchlistManager;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;

/**
 * Controller for the Auction Marketplace screen ("Sàn đấu giá").
 *
 * Implements the new requirements:
 *  - Marketplace-style layout (categories sidebar + product cards in grid).
 *  - Tabs: All / My active bids / Watchlist.
 *  - Avatar button next to logout opens ProfileDialog.
 *  - When clicking a card, navigates to AuctionDetail (entering the auction room).
 *  - Listens for BID_UPDATE / AUCTION_END to drive notifications and refresh
 *    the "My bids" tab automatically.
 *
 * Note: We keep the existing PRODUCT_LIST contract with the server unchanged.
 */
public class AuctionListController {

    // ============== FXML fields ==============
    @FXML private TextField searchField;
    @FXML private Label userLabel;
    @FXML private Button avatarBtn;
    @FXML private Button logoutBtn;
    @FXML private Button homeBtn;
    @FXML private Button refreshBtn;
    @FXML private Label tabHintLabel;

    @FXML private VBox categoryBox;          // matches <VBox fx:id="categoryBox"> in FXML
    @FXML private TabPane tabPane;
    @FXML private FlowPane allFlowPane;
    @FXML private FlowPane watchFlowPane;
    @FXML private VBox myBidsBox;
    @FXML private Label myBidsHint;

    // ============== State ==============
    private final SocketClient client = SocketClient.getInstance();
    private ServerListener listener;
    private final List<ProductRow> products = new ArrayList<>();
    private final SimpleDateFormat displayFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private String currentCategoryFilter = "ALL";

    // Subscriptions
    private java.util.function.Consumer<Set<Integer>> watchSub;
    private java.util.function.Consumer<Map<Integer, MyBidsManager.Entry>> mybidsSub;

    private static final String[] CATEGORIES = {
            "ALL", "ELECTRONICS", "ART", "VEHICLE"
    };
    private static final String[] CATEGORY_LABELS = {
            "Tất cả danh mục", "Điện tử", "Nghệ thuật", "Phương tiện"
    };

    @FXML
    public void initialize() {
        // Header
        User u = Session.getInstance().getCurrentUser();
        if (u != null) {
            userLabel.setText("Xin chào, " + u.getAccountname()
                    + (u.getRole() == null ? "" : " (" + u.getRole() + ")"));
            avatarBtn.setText(initials(u));
        }
        if (homeBtn != null && (u == null || u.getRole() == null
                || !"MEMBER".equalsIgnoreCase(u.getRole().name()))) {
            homeBtn.setVisible(false);
            homeBtn.setManaged(false);
        }

        // Categories — build toggle buttons inside categoryBox
        if (categoryBox != null) {
            ToggleGroup catGroup = new ToggleGroup();
            for (int i = 0; i < CATEGORIES.length; i++) {
                final int idx = i;
                ToggleButton tb = new ToggleButton(CATEGORY_LABELS[i]);
                tb.getStyleClass().add("category-btn");
                tb.setMaxWidth(Double.MAX_VALUE);
                tb.setAlignment(Pos.CENTER_LEFT);
                tb.setToggleGroup(catGroup);
                if (i == 0) tb.setSelected(true);
                tb.setOnAction(e -> {
                    // Always keep one category selected.
                    if (!tb.isSelected()) { tb.setSelected(true); return; }
                    currentCategoryFilter = CATEGORIES[idx];
                    renderAll();
                    renderWatch();
                });
                categoryBox.getChildren().add(tb);
            }
        }

        // Search
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> {
                renderAll();
                renderWatch();
            });
        }

        // Tabs hint text
        if (tabPane != null) {
            tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldI, newI) -> {
                int i = newI == null ? 0 : newI.intValue();
                switch (i) {
                    case 0 -> tabHintLabel.setText("Tất cả phiên đấu giá");
                    case 1 -> tabHintLabel.setText("Sản phẩm bạn đang đấu giá");
                    case 2 -> tabHintLabel.setText("Sản phẩm bạn quan tâm");
                    default -> tabHintLabel.setText("");
                }
            });
        }

        // Subscribers
        watchSub = ids -> Platform.runLater(this::renderWatch);
        WatchlistManager.getInstance().subscribe(watchSub);

        mybidsSub = m -> Platform.runLater(this::renderMyBids);
        MyBidsManager.getInstance().subscribe(mybidsSub);

        // Network listener
        listener = this::handleResponse;
        client.addListener(listener);

        // Initial render of currently-known data
        renderMyBids();
        renderWatch();

        // Load auctions from server
        loadAuctions();
    }

    // ============================================================
    // Actions
    // ============================================================
    @FXML
    private void handleRefresh() {
        loadAuctions();
    }

    @FXML
    private void handleLogout() {
        client.send(new Request(MessageType.LOGOUT, null));
        Session.getInstance().logout();
        cleanup();
        SceneRouter.go("/view/Login.fxml", "Đăng nhập");
    }

    @FXML
    private void handleOpenProfile() {
        ProfileDialog.show();
        User u = Session.getInstance().getCurrentUser();
        if (u != null) {
            userLabel.setText("Xin chào, " + u.getAccountname()
                    + (u.getRole() == null ? "" : " (" + u.getRole() + ")"));
            avatarBtn.setText(initials(u));
        }
    }

    @FXML
    private void handleGoHome() {
        // Return MEMBER to the user dashboard.
        User u = Session.getInstance().getCurrentUser();
        if (u == null || u.getRole() == null) return;
        if ("MEMBER".equalsIgnoreCase(u.getRole().name())) {
            cleanup();
            SceneRouter.go("/view/UserDashboard.fxml", "Trang người dùng");
        }
    }

    @FXML
    private void handleGoMyProducts() {
        cleanup();
        SceneRouter.go("/view/MyProducts.fxml", "Sản phẩm của tôi");
    }

    /**
     * Open the bidding room. The argument is the productId (what ProductCard
     * tracks), but the server's JOIN_AUCTION_ROOM and BID_PLACE both want
     * the {@code auctionId} (the row in the auctions table). Resolve it
     * from {@link ProductRow} which we populated from the PRODUCT_LIST
     * response.
     */
    private void openDetail(int productId) {
        ProductRow r = findRow(productId);
        int auctionId = (r != null && r.auctionId > 0) ? r.auctionId : productId;
        cleanup();
        final int aId = auctionId;
        SceneRouter.go("/view/AuctionDetail.fxml",
                "Phiên đấu giá #" + aId,
                (AuctionDetailController c) -> c.setAuctionId(aId));
    }

    private void joinAuction(int productId) {
        // Same target as openDetail — the auction detail screen IS the bidding room.
        // But we also register interest in MyBidsManager so the user sees it in their tab.
        ProductRow r = findRow(productId);
        if (r != null) {
            WatchlistManager.getInstance().add(productId);
        }
        openDetail(productId);
    }

    // ============================================================
    // Network
    // ============================================================
    private void loadAuctions() {
        client.send(new Request(MessageType.PRODUCT_LIST, null));
    }

    private void handleResponse(Response resp) {
        MessageType t = resp.getType();
        if (t == null) return;

        switch (t) {
            case PRODUCT_LIST -> Platform.runLater(() -> onProductList(resp));
            case BID_UPDATE   -> Platform.runLater(() -> onBidUpdate(resp));
            case AUCTION_END  -> Platform.runLater(() -> onAuctionEnd(resp));
            default -> { /* ignore */ }
        }
    }

    @SuppressWarnings("unchecked")
    private void onProductList(Response resp) {
        if (!resp.isSuccess()) {
            NotificationService.getInstance().error(
                    "Không tải được danh sách",
                    resp.getMessage() == null ? "Lỗi không xác định" : resp.getMessage());
            return;
        }
        products.clear();
        String raw = JsonConverter.toJson(resp.getData());

        // Server returns PagedResponse<ProductResponse> with shape
        //   { items: [...], totalItems, currentPage, pageSize, totalPages }
        // — NOT a raw list. We try the paged shape first; if Gson finds an
        // "items" field we use that, otherwise we fall back to treating raw
        // as a list (in case the server ever returns a flat list directly).
        List<Map<String, Object>> items = null;
        try {
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> wrapper = new Gson().fromJson(raw, mapType);
            if (wrapper != null && wrapper.get("items") instanceof List<?> list) {
                items = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof Map<?, ?> mm) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> cast = (Map<String, Object>) mm;
                        items.add(cast);
                    }
                }
            }
        } catch (Exception ignored) { /* fall through to list-shape attempt */ }

        if (items == null) {
            // Fallback: raw is already a list
            try {
                Type listMapType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                items = new Gson().fromJson(raw, listMapType);
            } catch (Exception ex) {
                items = new ArrayList<>();
            }
        }
        if (items == null) items = new ArrayList<>();

        for (Map<String, Object> m : items) {
            products.add(ProductRow.fromMap(m, displayFmt));
        }
        renderAll();
        renderWatch();
        renderMyBids();
    }

    @SuppressWarnings("unchecked")
    private void onBidUpdate(Response resp) {
        if (!resp.isSuccess() || resp.getData() == null) return;
        String raw = JsonConverter.toJson(resp.getData());
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> m = new Gson().fromJson(raw, mapType);
        if (m == null) return;

        int pid = readInt(m.get("productId"));
        long price = readLong(m.get("currentPrice"));
        String name = readStr(m.get("productName"), readStr(m.get("name"), null));
        String leader = readStr(m.get("winnerAccountname"), readStr(m.get("currentLeader"), null));

        // Update local product row if present
        ProductRow row = findRow(pid);
        if (row != null) {
            row.currentPrice = price;
            row.currentLeader = leader;
            renderAll();
            renderWatch();
        }

        // Tell MyBidsManager so it can fire notifications
        User u = Session.getInstance().getCurrentUser();
        String me = u == null ? null : u.getAccountname();
        MyBidsManager.getInstance().onPriceUpdate(pid, name, price, leader, me);
    }

    @SuppressWarnings("unchecked")
    private void onAuctionEnd(Response resp) {
        if (resp.getData() == null) return;
        String raw = JsonConverter.toJson(resp.getData());
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> m = new Gson().fromJson(raw, mapType);
        if (m == null) return;
        int pid = readInt(m.get("productId"));
        long price = readLong(m.get("currentPrice"));
        String winner = readStr(m.get("winnerAccountname"), null);
        User u = Session.getInstance().getCurrentUser();
        String me = u == null ? null : u.getAccountname();
        MyBidsManager.getInstance().onAuctionEnd(pid, winner, price, me);

        // Update local row status
        ProductRow row = findRow(pid);
        if (row != null) {
            row.status = "FINISHED";
            row.currentPrice = price;
            row.currentLeader = winner;
            renderAll();
            renderWatch();
        }
    }

    // ============================================================
    // Rendering
    // ============================================================
    private void renderAll() {
        if (allFlowPane == null) return;
        allFlowPane.getChildren().clear();
        String search = searchField == null ? "" : safe(searchField.getText()).toLowerCase(Locale.ROOT);

        Predicate<ProductRow> filter = r -> matchesCategory(r) && matchesSearch(r, search);
        for (ProductRow r : products) {
            if (filter.test(r)) {
                allFlowPane.getChildren().add(buildCard(r));
            }
        }
        if (allFlowPane.getChildren().isEmpty()) {
            allFlowPane.getChildren().add(emptyHint("Chưa có phiên đấu giá phù hợp."));
        }
    }

    private void renderWatch() {
        if (watchFlowPane == null) return;
        watchFlowPane.getChildren().clear();
        Set<Integer> watched = WatchlistManager.getInstance().all();
        String search = searchField == null ? "" : safe(searchField.getText()).toLowerCase(Locale.ROOT);

        for (ProductRow r : products) {
            if (!watched.contains(r.productId)) continue;
            if (!matchesCategory(r)) continue;
            if (!matchesSearch(r, search)) continue;
            watchFlowPane.getChildren().add(buildCard(r));
        }
        if (watchFlowPane.getChildren().isEmpty()) {
            watchFlowPane.getChildren().add(emptyHint(
                    "Bạn chưa có sản phẩm nào trong danh sách quan tâm. Bấm ♡ trên thẻ sản phẩm để thêm."));
        }
    }

    private void renderMyBids() {
        if (myBidsBox == null) return;
        myBidsBox.getChildren().clear();
        Map<Integer, MyBidsManager.Entry> entries = MyBidsManager.getInstance().snapshot();
        if (entries.isEmpty()) {
            myBidsBox.getChildren().add(emptyHint(
                    "Bạn chưa tham gia phiên đấu giá nào. Khi bạn đặt giá, sản phẩm sẽ xuất hiện ở đây."));
            return;
        }
        for (MyBidsManager.Entry e : entries.values()) {
            myBidsBox.getChildren().add(buildMyBidRow(e));
        }
    }

    private boolean matchesCategory(ProductRow r) {
        if ("ALL".equalsIgnoreCase(currentCategoryFilter)) return true;
        return currentCategoryFilter.equalsIgnoreCase(r.category);
    }

    private boolean matchesSearch(ProductRow r, String search) {
        if (search == null || search.isEmpty()) return true;
        return r.name != null && r.name.toLowerCase(Locale.ROOT).contains(search);
    }

    private ProductCard buildCard(ProductRow r) {
        return new ProductCard(
                r.productId, r.name, r.category, r.currentPrice, r.status, r.imageUrl,
                this::openDetail, this::joinAuction);
    }

    private VBox buildMyBidRow(MyBidsManager.Entry e) {
        VBox row = new VBox(4);
        row.getStyleClass().add("mybid-row");
        row.setPadding(new Insets(12, 14, 12, 14));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(e.productName == null ? "Sản phẩm #" + e.productId : e.productName);
        name.getStyleClass().add("mybid-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label statusBadge = new Label(statusText(e.status));
        statusBadge.getStyleClass().addAll("status-badge", "mybid-status-" + e.status.name().toLowerCase(Locale.ROOT));
        header.getChildren().addAll(name, spacer, statusBadge);

        Label price = new Label("Giá hiện tại: " + String.format("%,d đ", e.currentPrice)
                + "   |   Giá của bạn: " + String.format("%,d đ", e.myLastBid));
        price.getStyleClass().add("mybid-price");

        Label leader = new Label(e.currentLeader == null
                ? "Chưa có người dẫn đầu"
                : "Đang dẫn đầu: " + e.currentLeader);
        leader.getStyleClass().add("muted-label");

        Label time = new Label("Cập nhật: " + new SimpleDateFormat("HH:mm:ss").format(new Date(e.lastUpdateMillis)));
        time.getStyleClass().add("muted-label");

        Button viewBtn = new Button("Vào phiên");
        viewBtn.getStyleClass().add("primary-btn");
        viewBtn.setOnAction(ev -> openDetail(e.productId));

        Button removeBtn = new Button("Bỏ theo dõi");
        removeBtn.getStyleClass().add("ghost-btn");
        removeBtn.setOnAction(ev -> MyBidsManager.getInstance().remove(e.productId));

        HBox actions = new HBox(8, viewBtn, removeBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(header, price, leader, time, actions);
        return row;
    }

    private Label emptyHint(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted-label");
        l.setWrapText(true);
        l.setPadding(new Insets(16));
        return l;
    }

    private static String statusText(MyBidsManager.Status s) {
        return switch (s) {
            case WINNING -> "Đang dẫn đầu";
            case OUTBID  -> "Đã bị vượt giá";
            case WON     -> "Thành công";
            case LOST    -> "Chưa thành công";
            case PENDING -> "Đang chờ";
        };
    }

    private ProductRow findRow(int productId) {
        for (ProductRow r : products) {
            if (r.productId == productId) return r;
        }
        return null;
    }

    private void cleanup() {
        client.removeListener(listener);
        if (watchSub != null) WatchlistManager.getInstance().unsubscribe(watchSub);
        if (mybidsSub != null) MyBidsManager.getInstance().unsubscribe(mybidsSub);
    }

    // ============================================================
    // Helpers
    // ============================================================
    private static String safe(String s) { return s == null ? "" : s; }

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

    private static String readStr(Object o, String fallback) {
        if (o == null) return fallback;
        String s = o.toString();
        return s.isEmpty() ? fallback : s;
    }

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

    // ============================================================
    // Row model
    // ============================================================
    public static class ProductRow {
        int productId;
        int auctionId;          // server's auction id — REQUIRED for JOIN_AUCTION_ROOM
        String name;
        String description;
        String category;
        long currentPrice;
        String status;
        String endTime;
        String currentLeader;
        String imageUrl;

        static ProductRow fromMap(Map<String, Object> m, SimpleDateFormat fmt) {
            ProductRow r = new ProductRow();
            r.productId    = readInt(m.get("productId"));
            r.auctionId    = readInt(m.get("auctionId"));
            r.name         = readStr(m.get("productName"), readStr(m.get("name"), ""));
            r.description  = readStr(m.get("description"), "");
            r.category     = readStr(m.get("category"), "OTHER");
            r.currentPrice = readLong(m.get("currentPrice"));
            r.status       = readStr(m.get("status"), "");
            r.imageUrl     = readStr(m.get("imageUrl"), null);
            r.currentLeader = readStr(m.get("winnerAccountname"), null);

            Object end = m.get("endTime");
            if (end != null) {
                if (end instanceof String s) {
                    try {
                        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date d = in.parse(s);
                        r.endTime = fmt.format(d);
                    } catch (Exception ex) {
                        r.endTime = s;
                    }
                } else {
                    r.endTime = end.toString();
                }
            } else {
                r.endTime = "";
            }
            return r;
        }
    }
}
