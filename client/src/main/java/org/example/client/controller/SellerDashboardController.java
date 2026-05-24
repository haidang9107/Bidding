package org.example.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.session.Session;
import org.example.client.util.SceneRouter;
import org.example.model.enums.ItemCategory;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller cho màn hình Seller Dashboard.
 *
 * Người bán có thể tạo phiên đấu giá mới. Yêu cầu được gửi với MessageType.PRODUCT_ADD
 * và payload là JSON của một Map chứa tất cả thông tin sản phẩm cùng các trường đặc
 * thù theo loại (Electronics/Art/Vehicle).
 *
 * Server có thể chưa cài đặt đầy đủ PRODUCT_ADD; phía client vẫn gửi đúng cấu trúc
 * để khi server bổ sung là dùng được ngay.
 */
public class SellerDashboardController {

    @FXML private TextField nameField;
    @FXML private TextArea descArea;
    @FXML private TextField startingPriceField;
    @FXML private TextField bidIncrementField;
    @FXML private TextField durationField;
    @FXML private ChoiceBox<String> categoryBox;
    @FXML private TextField brandField;
    @FXML private TextField warrantyField;
    @FXML private TextField artistField;
    @FXML private TextField yearField;
    @FXML private TextField modelField;

    @FXML private Button createBtn;
    @FXML private Button viewAuctionsBtn;
    @FXML private Button logoutBtn;
    @FXML private Label userLabel;
    @FXML private Label statusLabel;

    private final SocketClient client = SocketClient.getInstance();
    private ServerListener listener;

    @FXML
    public void initialize() {
        User u = Session.getInstance().getCurrentUser();
        if (u != null) {
            userLabel.setText("Người bán: " + u.getAccountname());
        }

        categoryBox.setItems(FXCollections.observableArrayList(
                ItemCategory.ELECTRONICS.name(),
                ItemCategory.ART.name(),
                ItemCategory.VEHICLE.name()
        ));
        categoryBox.setValue(ItemCategory.ELECTRONICS.name());

        listener = this::handleResponse;
        client.addListener(listener);
    }

    @FXML
    private void handleCreate() {
        // Đọc và validate dữ liệu cơ bản
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String desc = descArea.getText() == null ? "" : descArea.getText().trim();
        String cat = categoryBox.getValue();
        if (name.isEmpty()) {
            statusLabel.setText("Tên sản phẩm không được trống!");
            return;
        }

        long startingPrice;
        long stepPrice;
        long durationMinutes;
        try {
            startingPrice = Long.parseLong(startingPriceField.getText().trim());
            stepPrice = Long.parseLong(bidIncrementField.getText().trim());
            durationMinutes = Long.parseLong(durationField.getText().trim());
        } catch (NumberFormatException e) {
            statusLabel.setText("Giá khởi điểm, bước giá và thời lượng phải là số!");
            return;
        }
        if (startingPrice < 0 || stepPrice <= 0 || durationMinutes <= 0) {
            statusLabel.setText("Số liệu giá / thời lượng không hợp lệ!");
            return;
        }

        User seller = Session.getInstance().getCurrentUser();
        if (seller == null) {
            statusLabel.setText("Chưa đăng nhập!");
            return;
        }

        // Đóng gói payload kiểu Map cho dễ đọc ở server
        Map<String, Object> payload = new HashMap<>();
        payload.put("sellerAccountname", seller.getAccountname());
        payload.put("productName", name);
        payload.put("description", desc);
        payload.put("startingPrice", startingPrice);
        payload.put("stepPrice", stepPrice);
        payload.put("durationMinutes", durationMinutes);
        payload.put("category", cat);

        // Trường đặc thù theo loại
        if (ItemCategory.ELECTRONICS.name().equals(cat)) {
            payload.put("brand", safeText(brandField));
            payload.put("warrantyMonths", parseIntSafe(warrantyField, 0));
        } else if (ItemCategory.ART.name().equals(cat)) {
            payload.put("artist", safeText(artistField));
            payload.put("year", parseIntSafe(yearField, 0));
        } else if (ItemCategory.VEHICLE.name().equals(cat)) {
            payload.put("brand", safeText(brandField));
            payload.put("model", safeText(modelField));
            payload.put("manufactureYear", parseIntSafe(yearField, 0));
        }

        createBtn.setDisable(true);
        statusLabel.setText("Đang gửi yêu cầu...");
        client.send(new Request(MessageType.PRODUCT_ADD, JsonConverter.toJson(payload)));
    }

    @FXML
    private void handleViewAuctions() {
        cleanup();
        SceneRouter.go("/view/AuctionList.fxml", "Danh sách phiên đấu giá");
    }

    @FXML
    private void handleLogout() {
        client.send(new Request(MessageType.LOGOUT, null));
        Session.getInstance().logout();
        cleanup();
        SceneRouter.go("/view/Login.fxml", "Đăng nhập");
    }

    private void handleResponse(Response resp) {
        MessageType t = resp.getType();
        // Chỉ quan tâm phản hồi liên quan đến PRODUCT_ADD: trả về SUCCESS hoặc ERROR
        if (t != MessageType.SUCCESS && t != MessageType.ERROR && t != MessageType.PRODUCT_ADD) return;

        Platform.runLater(() -> {
            createBtn.setDisable(false);
            if (resp.isSuccess()) {
                statusLabel.setText("✓ Tạo phiên thành công!");
                clearForm();
            } else {
                statusLabel.setText("✗ " + resp.getMessage());
            }
        });
    }

    private void clearForm() {
        nameField.clear();
        descArea.clear();
        startingPriceField.clear();
        bidIncrementField.clear();
        durationField.clear();
        brandField.clear();
        warrantyField.clear();
        artistField.clear();
        yearField.clear();
        modelField.clear();
    }

    private void cleanup() {
        client.removeListener(listener);
    }

    private String safeText(TextField f) {
        return f.getText() == null ? "" : f.getText().trim();
    }

    private int parseIntSafe(TextField f, int def) {
        try {
            return Integer.parseInt(safeText(f));
        } catch (Exception e) {
            return def;
        }
    }
}
