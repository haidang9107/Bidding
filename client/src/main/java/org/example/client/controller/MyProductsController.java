package org.example.client.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.client.dialog.OpenAuctionDialog;
import org.example.client.dialog.ProfileDialog;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.notification.NotificationService;
import org.example.client.session.Session;
import org.example.client.util.SceneRouter;
import org.example.dto.request.PaginationRequest;
import org.example.dto.request.ProductUpdateRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Controller for the "Sản phẩm của tôi" screen.
 *
 * <p>Server does not expose a dedicated "my-products" endpoint, so this screen
 * fetches the full auction list via {@code PRODUCT_LIST} and filters client-side
 * by {@code sellerAccountname == currentUser.accountname}.
 *
 * <p>Auctions are split across three tabs:
 * <ul>
 *   <li>OPEN → "Trong kho" (chưa bắt đầu)</li>
 *   <li>RUNNING → "Đang đấu giá"</li>
 *   <li>FINISHED / PAID / CANCELED → "Đã bán / Lịch sử"</li>
 * </ul>
 */
public class MyProductsController {

    // === FXML ===
    @FXML private Label userLabel;
    @FXML private Label summaryLabel;
    @FXML private Button backBtn;
    @FXML private Button marketBtn;
    @FXML private Button logoutBtn;
    @FXML private Button avatarBtn;
    @FXML private Button refreshBtn;
    @FXML private Button addBtn;
    @FXML private TabPane tabPane;
    @FXML private VBox stockBox;
    @FXML private VBox auctionBox;
    @FXML private VBox soldBox;

    private final SocketClient client = SocketClient.getInstance();
    /** True while waiting for a PRODUCT_UPDATE/PRODUCT_WITHDRAW reply so we
     *  can route the generic SUCCESS/ERROR envelope correctly. */
    private boolean awaitingMutation = false;
    private ServerListener listener;
    private final SimpleDateFormat displayFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        User u = Session.getInstance().getCurrentUser();
        if (u != null) {
            userLabel.setText("Xin chào, " + u.getAccountname());
            avatarBtn.setText(initials(u));
        }

        listener = this::handleResponse;
        client.addListener(listener);

        loadProducts();
    }

    // ============================================================
    // Actions
    // ============================================================
    @FXML
    private void handleBack() {
        cleanup();
        SceneRouter.go("/view/UserDashboard.fxml", "Trang người dùng");
    }

    @FXML
    private void handleGoMarket() {
        cleanup();
        SceneRouter.go("/view/AuctionList.fxml", "Sàn đấu giá trực tuyến");
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
        if (u != null && avatarBtn != null) avatarBtn.setText(initials(u));
    }

    @FXML
    private void handleRefresh() {
        loadProducts();
    }

    @FXML
    private void handleAddProduct() {
        cleanup();
        SceneRouter.go("/view/SellerDashboard.fxml", "Thêm sản phẩm");
    }

    // ============================================================
    // Network
    // ============================================================
    private void loadProducts() {
        // Load both:
        //  - MY_PRODUCT_LIST  → products in the seller's inventory (stock + running)
        //  - PRODUCT_LIST     → auctions; we filter by sellerAccountname to find ours
        // We render after each response so the user sees results as soon as they
        // arrive.
        client.send(new Request(MessageType.MY_PRODUCT_LIST, null));
        PaginationRequest pr = new PaginationRequest(1, 10);
        client.send(new Request(MessageType.PRODUCT_LIST, pr));
        summaryLabel.setText("Đang tải kho...");
    }

    private final List<Row> stockRows = new ArrayList<>();
    private final List<Row> auctionRows = new ArrayList<>();

    private void handleResponse(Response resp) {
        MessageType t = resp.getType();
        if (t == MessageType.MY_PRODUCT_LIST) {
            Platform.runLater(() -> onMyProductList(resp));
        } else if (t == MessageType.PRODUCT_LIST) {
            Platform.runLater(() -> onProductList(resp));
        } else if (t == MessageType.SUCCESS && awaitingMutation) {
            // Reply to our PRODUCT_UPDATE / PRODUCT_WITHDRAW.
            Platform.runLater(() -> {
                awaitingMutation = false;
                NotificationService.getInstance().info("Thành công",
                        resp.getMessage() == null ? "Đã cập nhật." : resp.getMessage());
                loadProducts();
            });
        } else if (t == MessageType.ERROR && awaitingMutation) {
            Platform.runLater(() -> {
                awaitingMutation = false;
                NotificationService.getInstance().error("Thất bại",
                        resp.getMessage() == null ? "Không thực hiện được." : resp.getMessage());
            });
        }
    }

    /** Send PRODUCT_WITHDRAW {productId}. */
    private void sendWithdraw(int productId) {
        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        payload.addProperty("productId", productId);
        awaitingMutation = true;
        client.send(new Request(MessageType.PRODUCT_WITHDRAW, payload));
    }

    /** Simple edit dialog → PRODUCT_UPDATE with ProductUpdateRequest. */
    private void showEditDialog(Row r) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Sửa sản phẩm");
        dlg.setHeaderText("Sửa thông tin '" + safe(r.name) + "'");
        ButtonType saveType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField nameField = new TextField(safe(r.name));
        TextArea descField = new TextArea("");
        descField.setPromptText("Mô tả mới (để trống nếu không đổi)");
        descField.setPrefRowCount(3);
        TextField imageField = new TextField(r.imageUrl == null ? "" : r.imageUrl);

        GridPane gp = new GridPane();
        gp.setHgap(8); gp.setVgap(8);
        gp.add(new Label("Tên:"), 0, 0);        gp.add(nameField, 1, 0);
        gp.add(new Label("Mô tả:"), 0, 1);      gp.add(descField, 1, 1);
        gp.add(new Label("URL ảnh:"), 0, 2);    gp.add(imageField, 1, 2);
        dlg.getDialogPane().setContent(gp);

        Optional<ButtonType> res = dlg.showAndWait();
        if (res.isEmpty() || res.get() != saveType) return;

        ProductUpdateRequest req = new ProductUpdateRequest();
        req.setProductId(r.productId);
        req.setName(nameField.getText() == null ? "" : nameField.getText().trim());
        String desc = descField.getText() == null ? "" : descField.getText().trim();
        if (!desc.isEmpty()) req.setDescription(desc);
        String url = imageField.getText() == null ? "" : imageField.getText().trim();
        if (!url.isEmpty()) req.setImageUrl(url);
        awaitingMutation = true;
        client.send(new Request(MessageType.PRODUCT_UPDATE, req));
    }

    @SuppressWarnings("unchecked")
    private void onMyProductList(Response resp) {
        stockRows.clear();
        if (!resp.isSuccess()) {
            NotificationService.getInstance().error(
                    "Không tải được kho",
                    resp.getMessage() == null ? "Lỗi" : resp.getMessage());
            render();
            return;
        }
        // resp.getData() is a List<ProductResponse> serialised back to LinkedHashMap list.
        String raw = JsonConverter.toJson(resp.getData());
        try {
            Type listMapType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> items = new Gson().fromJson(raw, listMapType);
            if (items == null) items = new ArrayList<>();
            for (Map<String, Object> m : items) {
                // Inventory rows: only show items NOT currently in an auction here.
                // Items in auction are picked up from the PRODUCT_LIST response.
                Object flag = m.get("inAuction");
                boolean inAuction = flag instanceof Boolean ? (Boolean) flag
                        : "true".equalsIgnoreCase(String.valueOf(flag));
                if (inAuction) continue;
                Row r = Row.fromMap(m, displayFmt);
                r.status = "STOCK"; // explicit, even though there is no auction status
                stockRows.add(r);
            }
        } catch (Exception ignored) { /* leave stockRows empty */ }
        render();
    }

    @SuppressWarnings("unchecked")
    private void onProductList(Response resp) {
        auctionRows.clear();
        if (!resp.isSuccess()) {
            render();
            return;
        }
        User me = Session.getInstance().getCurrentUser();
        if (me == null) return;
        String myAcc = me.getAccountname();

        String raw = JsonConverter.toJson(resp.getData());
        List<Map<String, Object>> items = extractItems(raw);

        for (Map<String, Object> m : items) {
            String seller = readStr(m.get("sellerAccountname"), readStr(m.get("ownerAccountname"), ""));
            if (!myAcc.equalsIgnoreCase(seller)) continue;
            auctionRows.add(Row.fromMap(m, displayFmt));
        }
        render();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(String raw) {
        // Server returns PagedResponse {items: [...], ...}
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

    // ============================================================
    // Rendering
    // ============================================================
    private void render() {
        stockBox.getChildren().clear();
        auctionBox.getChildren().clear();
        soldBox.getChildren().clear();

        int nStock = stockRows.size();
        int nRunning = 0, nDone = 0;

        // Stock tab: items in seller's inventory
        for (Row r : stockRows) {
            stockBox.getChildren().add(buildRow(r, true));
        }

        // Auction tabs: split by status
        for (Row r : auctionRows) {
            switch (r.statusCategory()) {
                case STOCK    -> { /* on-marketplace product not yet started — show under "Đang đấu giá" */
                    auctionBox.getChildren().add(buildRow(r, false));
                    nRunning++;
                }
                case RUNNING  -> { auctionBox.getChildren().add(buildRow(r, false)); nRunning++; }
                case DONE     -> { soldBox.getChildren().add(buildRow(r, false));    nDone++; }
            }
        }

        if (stockBox.getChildren().isEmpty())   stockBox.getChildren().add(emptyHint("Kho trống. Bấm '+ Thêm sản phẩm' để thêm mới."));
        if (auctionBox.getChildren().isEmpty()) auctionBox.getChildren().add(emptyHint("Chưa có phiên đang chạy."));
        if (soldBox.getChildren().isEmpty())    soldBox.getChildren().add(emptyHint("Chưa có lịch sử."));

        summaryLabel.setText(String.format(
                "Kho: %d • Đang đấu giá: %d • Lịch sử: %d", nStock, nRunning, nDone));
    }

    private HBox buildRow(Row r, boolean isStock) {
        // Outer container: thumbnail on the left, info + actions on the right.
        HBox box = new HBox(14);
        box.getStyleClass().add("product-row");
        box.setPadding(new Insets(12, 14, 12, 14));
        box.setAlignment(Pos.CENTER_LEFT);

        // ===== Thumbnail =====
        javafx.scene.layout.StackPane thumb = new javafx.scene.layout.StackPane();
        thumb.getStyleClass().add("product-thumb-bg");
        thumb.setMinSize(120, 90);
        thumb.setPrefSize(120, 90);
        if (r.imageUrl != null && !r.imageUrl.isEmpty()) {
            try {
                // Async load (true) so Cloudinary fetches don't block the UI.
                javafx.scene.image.Image img = new javafx.scene.image.Image(
                        r.imageUrl, 120, 90, true, true, true);
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(120);
                iv.setFitHeight(90);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                img.errorProperty().addListener((obs, was, isErr) -> {
                    if (Boolean.TRUE.equals(isErr)) iv.setVisible(false);
                });
                thumb.getChildren().add(iv);
            } catch (Exception ignored) { /* fallback: just gradient bg */ }
        }

        // ===== Right column =====
        VBox right = new VBox(6);
        HBox.setHgrow(right, javafx.scene.layout.Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(r.name == null || r.name.isEmpty()
                ? "Sản phẩm #" + r.productId : r.name);
        name.getStyleClass().add("product-row-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label badge = new Label(r.status);
        badge.getStyleClass().addAll("status-badge", "status-" + r.status.toLowerCase(Locale.ROOT));
        header.getChildren().addAll(name, spacer, badge);

        Label meta;
        if (isStock) {
            // Stock items have no auction yet — show only what's known.
            meta = new Label(String.format("Loại: %s   •   Trong kho — chưa đăng lên sàn",
                    r.category));
        } else {
            meta = new Label(String.format(
                    "Loại: %s   •   Giá hiện tại: %,d đ   •   Bắt đầu: %s   •   Kết thúc: %s",
                    r.category, r.currentPrice, r.startTime, r.endTime));
        }
        meta.getStyleClass().add("muted-label");
        meta.setWrapText(true);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (isStock) {
            // Stock items: open-auction, edit, withdraw.
            Button openBtn = new Button("Đăng lên sàn");
            openBtn.getStyleClass().add("primary-btn");
            openBtn.setOnAction(ev -> OpenAuctionDialog.show(r.productId, r.name, ok -> {
                if (Boolean.TRUE.equals(ok)) {
                    NotificationService.getInstance().info(
                            "Đăng thành công",
                            "Sản phẩm '" + (r.name == null ? "#" + r.productId : r.name)
                                    + "' đã lên sàn.");
                    loadProducts();
                }
            }));

            Button editBtn = new Button("Sửa");
            editBtn.getStyleClass().add("ghost-btn");
            editBtn.setOnAction(ev -> showEditDialog(r));

            Button withdrawBtn = new Button("Rút khỏi kho");
            withdrawBtn.getStyleClass().add("ghost-btn");
            withdrawBtn.setOnAction(ev -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Rút sản phẩm '" + safe(r.name) + "' khỏi kho?",
                        ButtonType.OK, ButtonType.CANCEL);
                confirm.setHeaderText(null);
                confirm.setTitle("Xác nhận");
                Optional<ButtonType> c = confirm.showAndWait();
                if (c.isPresent() && c.get() == ButtonType.OK) {
                    sendWithdraw(r.productId);
                }
            });

            actions.getChildren().addAll(openBtn, editBtn, withdrawBtn);
        } else {
            // "Xem chi tiết" opens a read-only product info dialog. It does
            // NOT enter the live bidding room.
            Button detailBtn = new Button("Xem chi tiết");
            detailBtn.getStyleClass().add("ghost-btn");
            detailBtn.setOnAction(ev ->
                    org.example.client.dialog.ProductDetailDialog.show(r.auctionId));

            // "Vào phòng đấu giá" enters the realtime room — placing a bid
            // is only possible from there.
            Button joinBtn = new Button("Vào phòng đấu giá");
            joinBtn.getStyleClass().add("primary-btn");
            joinBtn.setOnAction(ev -> {
                cleanup();
                int aid = r.auctionId;
                SceneRouter.go("/view/AuctionDetail.fxml",
                        "Phiên #" + aid,
                        (AuctionDetailController c) -> c.setAuctionId(aid));
            });
            actions.getChildren().addAll(detailBtn, joinBtn);
        }

        right.getChildren().addAll(header, meta, actions);
        box.getChildren().addAll(thumb, right);
        return box;
    }

    private Label emptyHint(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted-label");
        l.setWrapText(true);
        l.setPadding(new Insets(16));
        return l;
    }

    private void cleanup() {
        client.removeListener(listener);
    }

    // ============================================================
    // Helpers
    // ============================================================
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
        String[] parts = src.trim().split("\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 2; i++) {
            if (!parts[i].isEmpty()) sb.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private enum StatusCat { STOCK, RUNNING, DONE }

    public static class Row {
        int productId;
        int auctionId;
        String name;
        String category;
        long currentPrice;
        String status;
        String startTime;
        String endTime;
        String imageUrl;

        StatusCat statusCategory() {
            if (status == null) return StatusCat.DONE;
            return switch (status.toUpperCase(Locale.ROOT)) {
                case "OPEN"    -> StatusCat.STOCK;
                case "RUNNING" -> StatusCat.RUNNING;
                default        -> StatusCat.DONE;
            };
        }

        static Row fromMap(Map<String, Object> m, SimpleDateFormat fmt) {
            Row r = new Row();
            r.productId = readInt(m.get("productId"));
            r.auctionId = readInt(m.get("auctionId"));
            r.name = readStr(m.get("name"), readStr(m.get("productName"), ""));
            r.category = readStr(m.get("category"), "OTHER");
            r.currentPrice = readLong(m.get("currentPrice"));
            r.status = readStr(m.get("status"), "");
            r.startTime = formatTs(m.get("startTime"), fmt);
            r.endTime = formatTs(m.get("endTime"), fmt);
            r.imageUrl = readStr(m.get("imageUrl"), null);
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
}
