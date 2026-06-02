package org.example.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.client.network.ServerListener;
import org.example.client.network.SocketClient;
import org.example.client.session.Session;
import org.example.client.util.CloudinaryUploader;
import org.example.client.util.SceneRouter;
import org.example.dto.request.ProductCreateRequest;
import org.example.model.enums.ItemCategory;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the "Add to inventory" screen.
 *
 * <p>Two-step seller flow (per BTL requirements):
 * <ol>
 *   <li>This screen lets the seller add a product to their own inventory:
 *       name, description, category, category-specific fields and an image.
 *       NO price or duration is asked here.</li>
 *   <li>Later, from "Sản phẩm của tôi" → "Trong kho", the seller picks a stock
 *       item and clicks "Đăng lên sàn"; a dialog there collects price and
 *       duration and opens the auction.</li>
 * </ol>
 */
public class SellerDashboardController {

    // ============== Common fields ==============
    @FXML private TextField nameField;
    @FXML private TextArea descArea;
    @FXML private ChoiceBox<String> categoryBox;

    // ============== Image picker ==============
    @FXML private Button chooseImageBtn;
    @FXML private Button clearImageBtn;
    @FXML private ImageView imagePreview;
    @FXML private Label imageHintLabel;
    @FXML private Label imageNameLabel;

    // ============== Category-specific panes ==============
    @FXML private VBox electronicsPane;
    @FXML private VBox artPane;
    @FXML private VBox vehiclePane;
    @FXML private VBox otherPane;

    @FXML private TextField brandField;          // electronics
    @FXML private TextField warrantyField;       // electronics

    @FXML private TextField artistField;         // art
    @FXML private TextField artTypeField;        // art

    @FXML private TextField vehicleBrandField;   // vehicle
    @FXML private TextField modelField;          // vehicle
    @FXML private TextField yearField;           // vehicle

    // ============== Misc ==============
    @FXML private Button createBtn;
    @FXML private Button viewAuctionsBtn;
    @FXML private Button backBtn;
    @FXML private Button logoutBtn;
    @FXML private Label userLabel;
    @FXML private Label statusLabel;

    private static final long MAX_IMAGE_BYTES = 4L * 1024 * 1024; // 4 MB

    private final SocketClient client = SocketClient.getInstance();
    private ServerListener listener;

    /** URL Cloudinary trả về sau khi upload xong. Null khi chưa chọn ảnh
     *  hoặc upload đang chạy. */
    private String pickedImageUrl;

    @FXML
    public void initialize() {
        User u = Session.getInstance().getCurrentUser();
        if (u != null) {
            userLabel.setText("Người bán: " + u.getAccountname());
        }

        categoryBox.setItems(FXCollections.observableArrayList(
                ItemCategory.ELECTRONICS.name(),
                ItemCategory.ART.name(),
                ItemCategory.VEHICLE.name(),
                ItemCategory.OTHER.name()
        ));
        categoryBox.valueProperty().addListener((obs, oldV, newV) -> updateCategoryPane(newV));
        categoryBox.setValue(ItemCategory.ELECTRONICS.name());
        updateCategoryPane(ItemCategory.ELECTRONICS.name());

        clearImage();

        listener = this::handleResponse;
        client.addListener(listener);
    }

    private void updateCategoryPane(String categoryName) {
        ItemCategory cat;
        try { cat = ItemCategory.valueOf(categoryName); }
        catch (Exception e) { cat = ItemCategory.OTHER; }
        setPaneVisible(electronicsPane, cat == ItemCategory.ELECTRONICS);
        setPaneVisible(artPane,         cat == ItemCategory.ART);
        setPaneVisible(vehiclePane,     cat == ItemCategory.VEHICLE);
        setPaneVisible(otherPane,       cat == ItemCategory.OTHER);
    }

    private void setPaneVisible(VBox pane, boolean visible) {
        if (pane == null) return;
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    // ============================================================
    // Image picker
    // ============================================================
    @FXML
    private void handleChooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn ảnh sản phẩm");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp"));
        Stage owner = chooseImageBtn != null && chooseImageBtn.getScene() != null
                ? (Stage) chooseImageBtn.getScene().getWindow() : null;
        File file = fc.showOpenDialog(owner);
        if (file == null) return;

        if (file.length() > MAX_IMAGE_BYTES) {
            statusLabel.setText("Ảnh vượt quá 4 MB, hãy chọn ảnh nhỏ hơn.");
            return;
        }

        // Preview locally right away so the user sees something while the
        // upload runs.
        try {
            Image preview = new Image(file.toURI().toString(), 176, 176, true, true);
            imagePreview.setImage(preview);
            imageHintLabel.setVisible(false);
            imageHintLabel.setManaged(false);
            imageNameLabel.setText(file.getName()
                    + "  (" + (file.length() / 1024) + " KB) — đang tải lên...");
        } catch (Exception ignored) { /* preview is best-effort */ }

        // Upload off the JavaFX thread. While it's running, disable the
        // "Thêm vào kho" button so the user can't submit without an image URL.
        chooseImageBtn.setDisable(true);
        if (clearImageBtn != null) clearImageBtn.setDisable(true);
        createBtn.setDisable(true);
        statusLabel.setText("Đang tải ảnh lên Cloudinary...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return CloudinaryUploader.upload(file);
            }
        };
        task.setOnSucceeded(e -> {
            pickedImageUrl = task.getValue();
            imageNameLabel.setText(file.getName()
                    + "  (" + (file.length() / 1024) + " KB) ✓");
            statusLabel.setText("Đã tải ảnh lên thành công.");
            chooseImageBtn.setDisable(false);
            if (clearImageBtn != null) clearImageBtn.setDisable(false);
            createBtn.setDisable(false);
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            pickedImageUrl = null;
            statusLabel.setText("✗ Lỗi tải ảnh: "
                    + (ex == null ? "(unknown)" : ex.getMessage()));
            // Roll back the preview so the user knows nothing was uploaded.
            clearImage();
            chooseImageBtn.setDisable(false);
            if (clearImageBtn != null) clearImageBtn.setDisable(false);
            createBtn.setDisable(false);
        });

        Thread t = new Thread(task, "cloudinary-upload");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleClearImage() { clearImage(); }

    private void clearImage() {
        pickedImageUrl = null;
        if (imagePreview != null) imagePreview.setImage(null);
        if (imageHintLabel != null) {
            imageHintLabel.setVisible(true);
            imageHintLabel.setManaged(true);
        }
        if (imageNameLabel != null) imageNameLabel.setText("");
    }

    // ============================================================
    // Add to inventory (no auction params)
    // ============================================================
    @FXML
    private void handleCreate() {
        String name = safeText(nameField);
        String desc = descArea.getText() == null ? "" : descArea.getText().trim();
        String catName = categoryBox.getValue();
        if (name.isEmpty()) {
            statusLabel.setText("Tên sản phẩm không được trống!");
            return;
        }

        ItemCategory category;
        try { category = ItemCategory.valueOf(catName); }
        catch (Exception ex) { category = ItemCategory.OTHER; }

        if (!validateCategoryFields(category)) return;

        ProductCreateRequest req = new ProductCreateRequest(name, desc, category);

        Map<String, Object> metadata = new HashMap<>();
        switch (category) {
            case ELECTRONICS -> {
                metadata.put("brand", safeText(brandField));
                metadata.put("warrantyMonths", parseIntSafe(warrantyField, 0));
            }
            case ART -> {
                metadata.put("artist", safeText(artistField));
                metadata.put("artType", safeText(artTypeField));
            }
            case VEHICLE -> {
                metadata.put("brand", safeText(vehicleBrandField));
                metadata.put("model", safeText(modelField));
                metadata.put("manufactureYear", parseIntSafe(yearField, 0));
            }
            case OTHER -> { /* none */ }
        }
        req.setMetadata(metadata);

        if (pickedImageUrl != null && !pickedImageUrl.isBlank()) {
            req.setImageUrl(pickedImageUrl);
        }

        createBtn.setDisable(true);
        statusLabel.setText("Đang thêm vào kho...");
        // Send object directly so the server's JsonConverter.convert can deserialize it.
        client.send(new Request(MessageType.PRODUCT_CREATE, req));
    }

    private boolean validateCategoryFields(ItemCategory cat) {
        switch (cat) {
            case ELECTRONICS -> {
                if (safeText(brandField).isEmpty()) {
                    statusLabel.setText("Hãy nhập hãng sản phẩm điện tử.");
                    return false;
                }
            }
            case ART -> {
                if (safeText(artistField).isEmpty()) {
                    statusLabel.setText("Hãy nhập tên nghệ sĩ.");
                    return false;
                }
            }
            case VEHICLE -> {
                if (safeText(vehicleBrandField).isEmpty() || safeText(modelField).isEmpty()) {
                    statusLabel.setText("Hãy nhập hãng và model phương tiện.");
                    return false;
                }
                int year = parseIntSafe(yearField, 0);
                if (year <= 1900 || year > 2100) {
                    statusLabel.setText("Năm sản xuất không hợp lệ.");
                    return false;
                }
            }
            case OTHER -> { /* nothing required */ }
        }
        return true;
    }

    // ============================================================
    // Navigation
    // ============================================================
    @FXML
    private void handleGoHome() {
        cleanup();
        SceneRouter.go("/view/UserDashboard.fxml", "Trang chủ");
    }

    @FXML
    private void handleViewAuctions() {
        cleanup();
        SceneRouter.go("/view/AuctionList.fxml", "Danh sách phiên đấu giá");
    }

    @FXML
    private void handleBack() {
        cleanup();
        SceneRouter.go("/view/MyProducts.fxml", "Sản phẩm của tôi");
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
        if (t != MessageType.SUCCESS && t != MessageType.ERROR && t != MessageType.PRODUCT_CREATE) return;

        Platform.runLater(() -> {
            createBtn.setDisable(false);
            if (resp.isSuccess()) {
                statusLabel.setText("✓ Đã thêm sản phẩm vào kho!");
                clearForm();
            } else {
                statusLabel.setText("✗ " + resp.getMessage());
            }
        });
    }

    private void clearForm() {
        nameField.clear();
        descArea.clear();
        brandField.clear();
        warrantyField.clear();
        artistField.clear();
        artTypeField.clear();
        vehicleBrandField.clear();
        modelField.clear();
        yearField.clear();
        clearImage();
    }

    private void cleanup() {
        client.removeListener(listener);
    }

    private String safeText(TextField f) {
        return f == null || f.getText() == null ? "" : f.getText().trim();
    }

    private int parseIntSafe(TextField f, int def) {
        try { return Integer.parseInt(safeText(f)); }
        catch (Exception e) { return def; }
    }
}
