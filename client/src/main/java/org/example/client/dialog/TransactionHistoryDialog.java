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
import org.example.client.session.Session;
import org.example.dto.request.PaginationRequest;
import org.example.dto.response.PagedResponse;
import org.example.model.Transaction;
import org.example.model.enums.MessageType;
import org.example.model.enums.TransactionType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.util.JsonConverter;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Modal dialog showing the user's full transaction history (paged) from the
 * server's persistent store. This uses the {@code TRANSACTION_HISTORY}
 * MessageType that was defined on the server (FinanceController +
 * TransactionHistoryCommand) but had no client UI before.
 *
 * <p>Pagination uses {@link PaginationRequest} and the server replies with a
 * {@link PagedResponse}{@code <Transaction>} embedded in the response data.</p>
 */
public final class TransactionHistoryDialog {

    private static final int PAGE_SIZE = 20;
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private TransactionHistoryDialog() {}

    public static void show() {
        User user = Session.getInstance().getCurrentUser();
        if (user == null) return;

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Lịch sử giao dịch");

        Label title = new Label("LỊCH SỬ GIAO DỊCH");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label info = new Label("Tài khoản: " + user.getAccountname());
        info.setStyle("-fx-text-fill: #c0c0d0; -fx-font-size: 12px;");

        // ===== Table =====
        TableView<Row> table = new TableView<>();
        table.setStyle(
                "-fx-background-color: #1a1a2e;"
              + "-fx-table-cell-border-color: #2a2a3e;"
              + "-fx-control-inner-background: #1a1a2e;"
              + "-fx-control-inner-background-alt: #20203a;");
        table.setPlaceholder(new Label("Chưa có giao dịch nào."));

        TableColumn<Row, String> colTime = col("Thời gian", "time", 150);
        TableColumn<Row, String> colType = col("Loại", "typeLabel", 110);
        TableColumn<Row, String> colSender = col("Từ", "sender", 120);
        TableColumn<Row, String> colReceiver = col("Đến", "receiver", 120);
        TableColumn<Row, String> colAmount = col("Số tiền", "amount", 130);
        TableColumn<Row, String> colDesc = col("Mô tả", "description", 220);

        table.getColumns().addAll(colTime, colType, colSender, colReceiver, colAmount, colDesc);

        ObservableList<Row> rows = FXCollections.observableArrayList();
        table.setItems(rows);
        table.setPrefWidth(900);
        table.setPrefHeight(420);

        // ===== Pagination controls =====
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

        // ===== Network: subscribe and fetch =====
        int[] currentPage = {1};
        int[] totalPages = {1};
        SocketClient sc = SocketClient.getInstance();

        ServerListener[] holder = new ServerListener[1];
        holder[0] = resp -> {
            if (resp.getType() != MessageType.TRANSACTION_HISTORY) return;
            Platform.runLater(() -> {
                if (!resp.isSuccess() || resp.getData() == null) {
                    statusLabel.setText("✗ " + (resp.getMessage() == null
                            ? "Không tải được dữ liệu" : resp.getMessage()));
                    prevBtn.setDisable(currentPage[0] <= 1);
                    nextBtn.setDisable(true);
                    return;
                }
                try {
                    // The server's TransactionHistoryCommand returns
                    // PagedResponse<Transaction>. Gson cannot infer generics
                    // from Object, so we re-serialize then deserialize with
                    // TypeToken.
                    String raw = JsonConverter.toJson(resp.getData());
                    Type pagedType =
                            new TypeToken<PagedResponse<Transaction>>(){}.getType();
                    PagedResponse<Transaction> paged =
                            new Gson().fromJson(raw, pagedType);
                    rows.clear();
                    if (paged != null && paged.getItems() != null) {
                        List<Transaction> items = paged.getItems();
                        for (Transaction t : items) {
                            rows.add(Row.from(t, user.getAccountname()));
                        }
                    }
                    int page = paged == null ? 1 : Math.max(1, paged.getCurrentPage());
                    int total = paged == null ? 1 : Math.max(1, paged.getTotalPages());
                    currentPage[0] = page;
                    totalPages[0] = total;
                    pageLabel.setText("Trang " + page + " / " + total);
                    prevBtn.setDisable(page <= 1);
                    nextBtn.setDisable(page >= total);
                    long count = paged == null ? 0L : paged.getTotalItems();
                    statusLabel.setText("Đã tải " + rows.size() + " / " + count + " giao dịch.");
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
            sc.send(new Request(MessageType.TRANSACTION_HISTORY,
                    new PaginationRequest(currentPage[0], PAGE_SIZE)));
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

        // Detach listener when closed so we don't keep getting callbacks.
        stage.setOnHidden(e -> sc.removeListener(holder[0]));

        // ===== Layout =====
        HBox pager = new HBox(8, prevBtn, pageLabel, nextBtn, spacer(), refreshBtn, closeBtn);
        pager.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(12, title, info, table, statusLabel, pager);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a1a2e, #16213e);");

        Scene scene = new Scene(root);
        stage.setScene(scene);

        // Initial fetch then show.
        Platform.runLater(fetch);
        stage.show();
    }

    // ============================================================
    // Helpers
    // ============================================================
    private static <T> TableColumn<Row, String> col(String label, String prop, double w) {
        TableColumn<Row, String> c = new TableColumn<>(label);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        c.setStyle("-fx-text-fill: white;");
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

    private static String formatPrice(long p) {
        return String.format("%,d đ", p);
    }

    // ============================================================
    // Row view-model for the TableView. Public getters are required
    // by PropertyValueFactory.
    // ============================================================
    public static class Row {
        private final SimpleStringProperty time;
        private final SimpleStringProperty typeLabel;
        private final SimpleStringProperty sender;
        private final SimpleStringProperty receiver;
        private final SimpleStringProperty amount;
        private final SimpleStringProperty description;

        public Row(String time, String typeLabel, String sender, String receiver,
                   String amount, String description) {
            this.time = new SimpleStringProperty(time);
            this.typeLabel = new SimpleStringProperty(typeLabel);
            this.sender = new SimpleStringProperty(sender);
            this.receiver = new SimpleStringProperty(receiver);
            this.amount = new SimpleStringProperty(amount);
            this.description = new SimpleStringProperty(description);
        }

        public static Row from(Transaction t, String me) {
            String when = t.getCreatedAt() == null
                    ? "—" : FMT.format(new Date(t.getCreatedAt().getTime()));
            TransactionType tt = t.getType();
            String typeLabel = tt == null ? "—" : switch (tt) {
                case DEPOSIT          -> "Nạp tiền";
                case WITHDRAW         -> "Rút tiền";
                case TRANSFER         -> "Chuyển khoản";
                case AUCTION_PAYMENT  -> "Thanh toán đấu giá";
                case REFUND           -> "Hoàn tiền";
            };
            // Sign the amount from the user's point of view: + when they
            // received money, − when they paid.
            String sender = t.getSenderAccountname() == null ? "—" : t.getSenderAccountname();
            String receiver = t.getReceiverAccountname() == null ? "—" : t.getReceiverAccountname();
            boolean outgoing = me != null && me.equalsIgnoreCase(sender);
            boolean incoming = me != null && me.equalsIgnoreCase(receiver);
            String amount;
            if (outgoing && !incoming) {
                amount = "−" + formatPrice(t.getAmount());
            } else if (incoming && !outgoing) {
                amount = "+" + formatPrice(t.getAmount());
            } else {
                amount = formatPrice(t.getAmount());
            }
            return new Row(
                    when,
                    typeLabel,
                    sender,
                    receiver,
                    amount,
                    t.getDescription() == null ? "" : t.getDescription()
            );
        }

        public String getTime()        { return time.get(); }
        public String getTypeLabel()   { return typeLabel.get(); }
        public String getSender()      { return sender.get(); }
        public String getReceiver()    { return receiver.get(); }
        public String getAmount()      { return amount.get(); }
        public String getDescription() { return description.get(); }
    }
}
