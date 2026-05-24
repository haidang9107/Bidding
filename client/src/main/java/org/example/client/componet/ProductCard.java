package org.example.client.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.client.watchlist.WatchlistManager;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * A reusable card UI representing a single auction product in the marketplace.
 *
 * Layout (Tiki-inspired, simple/minimal):
 *  +-------------------------+
 *  |        IMAGE            |   <- 16:10 image area, fallback gradient if no URL
 *  |       (♥ icon)          |   <- watchlist toggle in the top-right
 *  |-------------------------|
 *  | Product name (2 lines)  |
 *  | Category badge          |
 *  | Current price           |
 *  | [Chi tiết] [Đấu giá]    |
 *  +-------------------------+
 *
 * NEW class — does not modify any existing controller.
 */
public class ProductCard extends VBox {

    private final int productId;

    public ProductCard(int productId,
                       String name,
                       String category,
                       long currentPrice,
                       String status,
                       String imageUrl,
                       Consumer<Integer> onDetail,
                       Consumer<Integer> onJoin) {
        this.productId = productId;
        getStyleClass().add("product-card");
        setSpacing(8);
        setPadding(new Insets(0, 0, 12, 0));
        setMinWidth(220);
        setPrefWidth(240);
        setMaxWidth(260);

        // === Image area with heart icon ===
        StackPane imageBox = buildImageBox(imageUrl);
        getChildren().add(imageBox);

        // === Body ===
        VBox body = new VBox(6);
        body.setPadding(new Insets(2, 12, 0, 12));

        Label nameLabel = new Label(safe(name, "Sản phẩm #" + productId));
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(220);

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label categoryBadge = new Label(safe(category, "OTHER"));
        categoryBadge.getStyleClass().add("category-badge");
        Label statusBadge = new Label(safe(status, ""));
        statusBadge.getStyleClass().add("status-badge");
        if (status != null && !status.isEmpty()) {
            statusBadge.getStyleClass().add("status-" + status.toLowerCase(Locale.ROOT));
        }
        metaRow.getChildren().addAll(categoryBadge, statusBadge);

        Label priceLabel = new Label(formatPrice(currentPrice));
        priceLabel.getStyleClass().add("price-label");

        Button detailBtn = new Button("Chi tiết");
        detailBtn.getStyleClass().add("card-detail-btn");
        detailBtn.setMaxWidth(Double.MAX_VALUE);
        detailBtn.setOnAction(e -> { if (onDetail != null) onDetail.accept(productId); });

        Button joinBtn = new Button("Đấu giá");
        joinBtn.getStyleClass().add("card-join-btn");
        joinBtn.setMaxWidth(Double.MAX_VALUE);
        joinBtn.setOnAction(e -> { if (onJoin != null) onJoin.accept(productId); });

        HBox actions = new HBox(8, detailBtn, joinBtn);
        HBox.setHgrow(detailBtn, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(joinBtn,   javafx.scene.layout.Priority.ALWAYS);

        body.getChildren().addAll(nameLabel, metaRow, priceLabel, actions);
        getChildren().add(body);
    }

    public int getProductId() {
        return productId;
    }

    // ============================================================
    // Helpers
    // ============================================================

    private StackPane buildImageBox(String imageUrl) {
        StackPane box = new StackPane();
        box.setMinHeight(140);
        box.setPrefHeight(150);
        box.setMaxHeight(150);
        box.getStyleClass().add("product-image-box");

        // Background — fallback gradient
        Region bg = new Region();
        bg.getStyleClass().add("product-image-bg");
        bg.prefWidthProperty().bind(box.widthProperty());
        bg.prefHeightProperty().bind(box.heightProperty());
        box.getChildren().add(bg);

        // Actual image (if URL provided)
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Image img = new Image(imageUrl, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(240);
                iv.setFitHeight(150);
                iv.setPreserveRatio(true);
                box.getChildren().add(iv);
            } catch (Exception ignored) {
                // keep fallback
            }
        }

        // Heart toggle overlay
        Button heart = new Button(WatchlistManager.getInstance().isWatched(productId) ? "♥" : "♡");
        heart.getStyleClass().add("heart-toggle");
        heart.setFocusTraversable(false);
        heart.setOnAction(e -> {
            WatchlistManager.getInstance().toggle(productId);
            heart.setText(WatchlistManager.getInstance().isWatched(productId) ? "♥" : "♡");
        });
        StackPane.setAlignment(heart, Pos.TOP_RIGHT);
        StackPane.setMargin(heart, new Insets(8, 8, 0, 0));
        box.getChildren().add(heart);

        return box;
    }

    private static String safe(String s, String fallback) {
        return s == null || s.isEmpty() ? fallback : s;
    }

    private static String formatPrice(long p) {
        return String.format("%,d đ", p);
    }
}
