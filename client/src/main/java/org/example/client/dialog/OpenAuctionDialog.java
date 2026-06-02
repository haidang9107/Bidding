package org.example.client.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.dto.request.AuctionOpenRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;

import java.sql.Timestamp;
import java.util.function.Consumer;

/**
 * Modal dialog that collects auction parameters (starting price, step price,
 * duration) for an existing inventory product and submits AUCTION_OPEN.
 *
 * <p>Used from {@code MyProductsController} when the seller clicks
 * "Đăng lên sàn" on a stock row.
 */
public final class OpenAuctionDialog {

    private OpenAuctionDialog() {}

    /**
     * Show the dialog and call {@code onResult} with true if the server
     * confirmed the auction opened, false if it failed or the dialog was
     * cancelled.
     */
    public static void show(int productId, String productName, Consumer<Boolean> onResult) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Đăng sản phẩm lên sàn");
        stage.setResizable(false);

        Label title = new Label("ĐĂNG LÊN SÀN ĐẤU GIÁ");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");

        Label nameLabel = new Label("Sản phẩm: " + (productName == null ? "#" + productId : productName));
        nameLabel.setStyle("-fx-text-fill: #c0c0d0; -fx-font-size: 13px;");

        TextField startingPriceField = new TextField();
        startingPriceField.setPromptText("Ví dụ: 1000000");
        TextField stepPriceField = new TextField();
        stepPriceField.setPromptText("Ví dụ: 50000");
        TextField durationField = new TextField();
        durationField.setPromptText("Ví dụ: 60 (phút)");
        styleField(startingPriceField);
        styleField(stepPriceField);
        styleField(durationField);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        int r = 0;
        grid.add(label("Giá khởi điểm (VNĐ)"), 0, r); grid.add(startingPriceField, 1, r++);
        grid.add(label("Bước giá tối thiểu (VNĐ)"), 0, r); grid.add(stepPriceField, 1, r++);
        grid.add(label("Thời lượng đấu giá (phút)"), 0, r); grid.add(durationField, 1, r++);

        Label status = new Label();
        status.setStyle("-fx-text-fill: #ffd166; -fx-font-size: 12px;");

        Button submitBtn = new Button("Đăng lên sàn");
        submitBtn.setStyle(
                "-fx-background-color: #5a8dee; -fx-text-fill: white;"
              + "-fx-font-weight: bold; -fx-padding: 10 18;"
              + "-fx-background-radius: 6; -fx-cursor: hand;");
        Button cancelBtn = new Button("Hủy");
        cancelBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #c0c0d0;"
              + "-fx-border-color: #3a3a4e; -fx-border-radius: 6;"
              + "-fx-padding: 8 14; -fx-cursor: hand;");

        final boolean[] resolved = {false};
        final ServerListener[] listenerHolder = new ServerListener[1];

        cancelBtn.setOnAction(e -> {
            if (listenerHolder[0] != null) SocketClient.getInstance().removeListener(listenerHolder[0]);
            stage.close();
            if (!resolved[0] && onResult != null) onResult.accept(false);
        });

        submitBtn.setOnAction(e -> {
            long startingPrice, stepPrice, durationMinutes;
            try {
                startingPrice = Long.parseLong(startingPriceField.getText().trim());
                stepPrice = Long.parseLong(stepPriceField.getText().trim());
                durationMinutes = Long.parseLong(durationField.getText().trim());
            } catch (NumberFormatException ex) {
                status.setText("Giá khởi điểm, bước giá và thời lượng phải là số!");
                return;
            }
            if (startingPrice <= 0 || stepPrice <= 0 || durationMinutes <= 0) {
                status.setText("Các giá trị phải lớn hơn 0!");
                return;
            }

            AuctionOpenRequest req = new AuctionOpenRequest(productId, startingPrice);
            req.setStepPrice(stepPrice);
            long now = System.currentTimeMillis();
            req.setStartTime(new Timestamp(now));
            req.setEndTime(new Timestamp(now + durationMinutes * 60_000L));

            submitBtn.setDisable(true);
            status.setText("Đang gửi yêu cầu...");

            ServerListener listener = resp -> {
                MessageType t = resp.getType();
                if (t != MessageType.SUCCESS && t != MessageType.ERROR && t != MessageType.AUCTION_OPEN) return;
                javafx.application.Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        resolved[0] = true;
                        SocketClient.getInstance().removeListener(listenerHolder[0]);
                        stage.close();
                        if (onResult != null) onResult.accept(true);
                    } else {
                        submitBtn.setDisable(false);
                        status.setText("✗ " + resp.getMessage());
                    }
                });
            };
            listenerHolder[0] = listener;
            SocketClient.getInstance().addListener(listener);
            SocketClient.getInstance().send(new Request(MessageType.AUCTION_OPEN, req));
        });

        HBox actions = new HBox(10, cancelBtn, submitBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(14, title, nameLabel, grid, status, actions);
        layout.setPadding(new Insets(24));
        layout.setMinWidth(420);
        layout.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a1a2e, #16213e);");

        Scene scene = new Scene(layout);
        stage.setScene(scene);
        stage.setOnHidden(ev -> {
            if (listenerHolder[0] != null) {
                SocketClient.getInstance().removeListener(listenerHolder[0]);
            }
            if (!resolved[0] && onResult != null) onResult.accept(false);
        });
        stage.showAndWait();
    }

    private static Label label(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill: #8a8aa0; -fx-font-size: 12px;");
        return l;
    }

    private static void styleField(TextField f) {
        f.setStyle(
                "-fx-background-color: #2a2a3e;"
              + "-fx-text-fill: white;"
              + "-fx-prompt-text-fill: #7a7a90;"
              + "-fx-padding: 8 10;"
              + "-fx-background-radius: 6;"
              + "-fx-font-size: 13px;");
        f.setPrefWidth(240);
    }
}
