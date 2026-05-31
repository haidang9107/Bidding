package org.example.client.dialog;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.dto.request.BidHistoryRequest;
import org.example.dto.response.PagedResponse;
import org.example.model.Bid;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.util.JsonConverter;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Modal dialog showing the persistent bid history of a single auction, fetched
 * from the server via the {@code BID_HISTORY} MessageType (server-side
 * BidController + BidHistoryCommand). Until now the client only ever saw bids
 * that arrived during the current room session via BID_UPDATE broadcasts; if
 * the user joined the room late they had no way to see earlier bids. This
 * dialog fills that gap.
 *
 * <p>Pagination uses {@link BidHistoryRequest} and the server replies with a
 * {@link PagedResponse}{@code <Bid>} inside the response data.</p>
 */
public final class BidHistoryDialog {

    private static final int PAGE_SIZE = 20;
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private BidHistoryDialog() {}

    public static void show(int auctionId, String productName) {
        if (auctionId <= 0) return;

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Lịch sử đặt giá");

        Label title = new Label("LỊCH SỬ ĐẶT GIÁ");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        String info = "Phiên #" + auctionId
                + (productName != null && !productName.isEmpty() ? " — " + productName : "");
        Label subtitle = new Label(info);
        subtitle.setStyle("-fx-text-fill: #c0c0d0; -fx-font-size: 12px;");

        // ===== Table =====
        TableView<Row> table = new TableView<>();
        table.setStyle(
                "-fx-background-color: #1a1a2e;"
              + "-fx-control-inner-background: #1a1a2e;"
              + "-fx-control-inner-background-alt: #20203a;");
        table.setPlaceholder(new Label("Chưa có lượt đặt giá nào trên phiên này."));

        TableColumn<Row, String> colTime = col("Thời gian", "time", 170);
        TableColumn<Row, String> colBidder = col("Người đặt", "bidder", 170);
        TableColumn<Row, String> colAmount = col("Giá đặt", "amount", 170);

        table.getColumns().addAll(colTime, colBidder, colAmount);

        ObservableList<Row> rows = FXCollections.observableArrayList();
        table.setItems(rows);
        table.setPrefWidth(620);
        table.setPrefHeight(420);

        // ===== Pagination =====
        Label statusLabel = new Label("Đang tải...");
        statusLabel.setStyle("-fx-text-fill: #8a8aa0; -fx-font-size: 12px;");

        Label pageLabel = new Label("Trang 1");
        pageLabel.setStyle("-fx-text-fill: #c0c0d0; -fx-font-size: 12px;");

        Button prevBtn = ghostBtn("← Trang trước");
        Button nextBtn = ghostBtn("Trang sau →");
        Button refreshBtn = ghostBtn("⟳ Làm mới");
        Button closeBtn = ghostBtn("Đóng");
        closeBtn.setOnAction(e -> stage.close());

        prevBtn.setDisable(true);
        nextBtn.setDisable(true);

        int[] currentPage = {1};
        int[] totalPages = {1};

        SocketClient sc = SocketClient.getInstance();
        ServerListener[] holder = new ServerListener[1];
        holder[0] = resp -> {
            if (resp.getType() != MessageType.BID_HISTORY) return;
            Platform.runLater(() -> {
                if (!resp.isSuccess() || resp.getData() == null) {
                    statusLabel.setText("✗ " + (resp.getMessage() == null
                            ? "Không tải được dữ liệu" : resp.getMessage()));
                    prevBtn.setDisable(currentPage[0] <= 1);
                    nextBtn.setDisable(true);
                    return;
                }
                try {
                    String raw = JsonConverter.toJson(resp.getData());
                    Type pagedType =
                            new TypeToken<PagedResponse<Bid>>(){}.getType();
                    PagedResponse<Bid> paged = new Gson().fromJson(raw, pagedType);
                    rows.clear();
                    if (paged != null && paged.getItems() != null) {
                        List<Bid> items = paged.getItems();
                        for (Bid b : items) rows.add(Row.from(b));
                    }
                    int page = paged == null ? 1 : Math.max(1, paged.getCurrentPage());
                    int total = paged == null ? 1 : Math.max(1, paged.getTotalPages());
                    currentPage[0] = page;
                    totalPages[0] = total;
                    pageLabel.setText("Trang " + page + " / " + total);
                    prevBtn.setDisable(page <= 1);
                    nextBtn.setDisable(page >= total);
                    long count = paged == null ? 0L : paged.getTotalItems();
                    statusLabel.setText("Đã tải " + rows.size() + " / " + count + " lượt.");
                } catch (Exception ex) {
                    statusLabel.setText("✗ Lỗi đọc dữ liệu: " + ex.getMessage());
                }
            });
        };
        sc.addListener(holder[0]);

        Runnable fetch = () -> {
            statusLabel.setText("Đang tải trang " + currentPage[0] + "...");
            prevBtn.setDisable(true);
            nextBtn.setDisable(true);
            sc.send(new Request(MessageType.BID_HISTORY,
                    new BidHistoryRequest(auctionId, currentPage[0], PAGE_SIZE)));
        };

        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 1) {
                currentPage[0]--;
                fetch.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage[0] < totalPages[0]) {
                currentPage[0]++;
                fetch.run();
            }
        });
        refreshBtn.setOnAction(e -> fetch.run());

        stage.setOnHidden(e -> sc.removeListener(holder[0]));

        // ===== Layout =====
        HBox pager = new HBox(8, prevBtn, pageLabel, nextBtn, spacer(), refreshBtn, closeBtn);
        pager.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(12, title, subtitle, table, statusLabel, pager);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a1a2e, #16213e);");

        Scene scene = new Scene(root);
        stage.setScene(scene);

        Platform.runLater(fetch);
        stage.show();
    }

    // ============================================================
    // Helpers
    // ============================================================
    private static TableColumn<Row, String> col(String label, String prop, double w) {
        TableColumn<Row, String> c = new TableColumn<>(label);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    private static Button ghostBtn(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: transparent;"
              + "-fx-text-fill: #c0c0d0;"
              + "-fx-border-color: #3a3a4e;"
              + "-fx-border-radius: 6;"
              + "-fx-padding: 6 12;"
              + "-fx-cursor: hand;");
        return b;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, javafx.scene.layout.Priority.ALWAYS);
        return r;
    }

    // ============================================================
    // Row view-model for the TableView.
    // ============================================================
    public static class Row {
        private final SimpleStringProperty time;
        private final SimpleStringProperty bidder;
        private final SimpleStringProperty amount;

        public Row(String time, String bidder, String amount) {
            this.time = new SimpleStringProperty(time);
            this.bidder = new SimpleStringProperty(bidder);
            this.amount = new SimpleStringProperty(amount);
        }

        public static Row from(Bid b) {
            String when = b.getBidTime() == null
                    ? "—" : FMT.format(new Date(b.getBidTime().getTime()));
            String who = b.getBidderAccountname() == null ? "—" : b.getBidderAccountname();
            return new Row(when, who, String.format("%,d đ", b.getBidAmount()));
        }

        public String getTime()   { return time.get(); }
        public String getBidder() { return bidder.get(); }
        public String getAmount() { return amount.get(); }
    }
}
