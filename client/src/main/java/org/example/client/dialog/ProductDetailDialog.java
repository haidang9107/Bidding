package org.example.client.dialog;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.dto.request.AuctionRoomRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Read-only modal dialog that shows everything the seller posted about a
 * product (name, description, image, category-specific specs, seller, price
 * range, time window). It deliberately does NOT join the auction room and
 * does NOT let the user place a bid — that's what AuctionDetail is for.
 *
 * <p>Usage:</p>
 * <pre>
 *   ProductDetailDialog.show(auctionId);
 * </pre>
 *
 * <p>The dialog fetches details from the server via {@code PRODUCT_DETAIL} so
 * it sees the same data as the bidding room. While the request is in flight
 * a placeholder is shown; on response the dialog populates the fields.</p>
 */
public final class ProductDetailDialog {

    private ProductDetailDialog() {
    }

    /**
     * Opens the dialog and fetches details for the given auction. Returns
     * immediately — the dialog is modal but data load is asynchronous.
     */
    public static void show(int auctionId) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Chi tiết sản phẩm");

        // === Title ===
        Label title = new Label("Đang tải thông tin...");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        // === Image holder ===
        StackPane imageBox = new StackPane();
        imageBox.setMinSize(360, 240);
        imageBox.setPrefSize(360, 240);
        imageBox.setMaxSize(360, 240);
        imageBox.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #2a2a3e, #1f1f33);"
              + "-fx-background-radius: 8;");
        Label imageHint = new Label("(Chưa có ảnh)");
        imageHint.setStyle("-fx-text-fill: #7a7a90; -fx-font-size: 13px;");
        imageBox.getChildren().add(imageHint);

        // === Info column ===
        VBox info = new VBox(10);
        info.setMinWidth(360);

        Label sellerLabel = field("Người bán:", "(đang tải)");
        Label categoryLabel = field("Loại:", "(đang tải)");
        Label statusLabel = field("Trạng thái:", "(đang tải)");
        Label startingPriceLabel = field("Giá khởi điểm:", "(đang tải)");
        Label stepPriceLabel = field("Bước giá:", "(đang tải)");
        Label currentPriceLabel = field("Giá hiện tại:", "(đang tải)");
        Label leaderLabel = field("Người dẫn đầu:", "(đang tải)");
        Label startTimeLabel = field("Bắt đầu:", "(đang tải)");
        Label endTimeLabel = field("Kết thúc:", "(đang tải)");
        Label specsLabel = field("Thông số:", "");

        Label descTitle = new Label("Mô tả");
        descTitle.setStyle("-fx-text-fill: #8a8aa0; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label descLabel = new Label("(đang tải)");
        descLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(360);

        info.getChildren().addAll(
                sellerLabel, categoryLabel, statusLabel,
                startingPriceLabel, stepPriceLabel, currentPriceLabel,
                leaderLabel, startTimeLabel, endTimeLabel, specsLabel,
                new Region(), descTitle, descLabel);

        // === Layout: image | info ===
        HBox content = new HBox(20, imageBox, info);
        content.setAlignment(Pos.TOP_LEFT);

        // === Footer ===
        Button closeBtn = new Button("Đóng");
        closeBtn.setStyle(
                "-fx-background-color: #5a8dee; -fx-text-fill: white;"
              + "-fx-font-weight: bold; -fx-padding: 10 24;"
              + "-fx-background-radius: 6; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> stage.close());

        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(16, title, content, footer);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a1a2e, #16213e);");

        ScrollPane scroll = new ScrollPane(layout);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1a1a2e; -fx-background-color: #1a1a2e;");

        Scene scene = new Scene(scroll, 820, 560);
        stage.setScene(scene);

        // === Wire up the listener BEFORE sending the request to avoid a race. ===
        SocketClient client = SocketClient.getInstance();
        ServerListener[] holder = new ServerListener[1];
        SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat outFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        holder[0] = resp -> {
            if (resp.getType() != MessageType.PRODUCT_DETAIL) return;
            Platform.runLater(() -> {
                if (!resp.isSuccess() || resp.getData() == null) {
                    title.setText("Không tải được thông tin");
                    return;
                }
                try {
                    String raw = JsonConverter.toJson(resp.getData());
                    Type t = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> m = new Gson().fromJson(raw, t);
                    int aid = readInt(m.get("auctionId"));
                    if (aid != auctionId) return; // Not for this dialog

                    populate(m, title, imageBox, imageHint,
                            sellerLabel, categoryLabel, statusLabel,
                            startingPriceLabel, stepPriceLabel, currentPriceLabel,
                            leaderLabel, startTimeLabel, endTimeLabel,
                            specsLabel, descLabel,
                            inFmt, outFmt);
                } catch (Exception ex) {
                    title.setText("Lỗi đọc dữ liệu");
                }
            });
        };
        client.addListener(holder[0]);

        // Always remove our listener when the dialog closes so it doesn't leak
        // and doesn't keep firing for future PRODUCT_DETAIL responses.
        stage.setOnHidden(e -> client.removeListener(holder[0]));

        // Fire the request.
        client.send(new Request(MessageType.PRODUCT_DETAIL, new AuctionRoomRequest(auctionId)));

        stage.showAndWait();
    }

    /**
     * Reads the server response map and writes the values into the UI labels.
     * Pulled out of {@link #show(int)} for readability — the method
     * is intentionally pure presentation logic.
     */
    private static void populate(Map<String, Object> m,
                                 Label title, StackPane imageBox, Label imageHint,
                                 Label seller, Label category, Label status,
                                 Label startingPrice, Label stepPrice, Label currentPrice,
                                 Label leader, Label startTime, Label endTime,
                                 Label specs, Label desc,
                                 SimpleDateFormat inFmt, SimpleDateFormat outFmt) {
        String name = readStr(m.get("name"), readStr(m.get("productName"), null));
        title.setText(name == null ? "Phiên #" + readInt(m.get("auctionId")) : name);

        // Image
        String imageUrl = readStr(m.get("imageUrl"), null);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Image img = new Image(imageUrl, 360, 240, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(360);
                iv.setFitHeight(240);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                img.errorProperty().addListener((obs, was, isErr) -> {
                    if (Boolean.TRUE.equals(isErr)) {
                        imageBox.getChildren().remove(iv);
                        imageHint.setText("(Không tải được ảnh)");
                    }
                });
                imageBox.getChildren().add(iv);
                imageHint.setVisible(false);
            } catch (Exception ignored) { /* keep placeholder */ }
        }

        // Top labels
        seller.setText(buildField("Người bán:",
                readStr(m.get("sellerAccountname"),
                        readStr(m.get("ownerAccountname"), "(không rõ)"))));
        category.setText(buildField("Loại:", readStr(m.get("category"), "OTHER")));
        status.setText(buildField("Trạng thái:", readStr(m.get("status"), "—")));

        long startP = readLong(m.get("startingPrice"));
        long stepP = readLong(m.get("stepPrice"));
        long curP  = readLong(m.get("currentPrice"));
        startingPrice.setText(buildField("Giá khởi điểm:", formatPrice(startP)));
        stepPrice.setText(buildField("Bước giá:", formatPrice(stepP)));
        currentPrice.setText(buildField("Giá hiện tại:", formatPrice(curP)));
        leader.setText(buildField("Người dẫn đầu:",
                readStr(m.get("winnerAccountname"), "(chưa có)")));

        startTime.setText(buildField("Bắt đầu:", formatTs(m.get("startTime"), inFmt, outFmt)));
        endTime.setText(buildField("Kết thúc:", formatTs(m.get("endTime"), inFmt, outFmt)));

        // Category-specific specs — best-effort, only show what we have.
        // The current server's ProductResponse DTO does NOT carry the
        // subclass-specific fields (brand / artist / model / etc), so this
        // section will usually be empty. We hide the row entirely in that
        // case rather than showing a useless "(không có)" placeholder.
        StringBuilder sb = new StringBuilder();
        appendSpec(sb, "Hãng", readStr(m.get("brand"), null));
        appendSpec(sb, "Bảo hành (tháng)", readStr(m.get("warrantyMonths"), null));
        appendSpec(sb, "Nghệ sĩ", readStr(m.get("artist"), null));
        appendSpec(sb, "Loại hình", readStr(m.get("artType"), null));
        appendSpec(sb, "Model", readStr(m.get("model"), null));
        appendSpec(sb, "Năm SX", readStr(m.get("manufactureYear"), null));
        if (sb.length() == 0) {
            specs.setVisible(false);
            specs.setManaged(false);
        } else {
            specs.setText(buildField("Thông số:", sb.toString()));
        }

        String d = readStr(m.get("description"), "");
        desc.setText(d.isEmpty() ? "(không có mô tả)" : d);
    }

    // ============================================================
    // Small UI helpers
    // ============================================================
    private static Label field(String label, String value) {
        Label l = new Label(buildField(label, value));
        l.setStyle("-fx-text-fill: #e0e0f0; -fx-font-size: 13px;");
        return l;
    }

    /**
     * Renders a "Label:  value" pair where the label tag and the value are in
     * a single Label control. Kept simple deliberately — the goal is a
     * compact, readable detail card, not a form.
     */
    private static String buildField(String label, String value) {
        return label + "  " + (value == null ? "—" : value);
    }

    private static String formatPrice(long p) {
        return String.format("%,d đ", p);
    }

    private static void appendSpec(StringBuilder sb, String label, String value) {
        if (value == null || value.isEmpty() || "0".equals(value)) return;
        if (sb.length() > 0) sb.append("  •  ");
        sb.append(label).append(": ").append(value);
    }

    private static String formatTs(Object o, SimpleDateFormat in, SimpleDateFormat out) {
        if (o == null) return "—";
        String s = o.toString();
        try {
            Date d = in.parse(s);
            return out.format(d);
        } catch (Exception ex) {
            return s;
        }
    }

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
}
