package org.example.server.model;

import java.sql.Timestamp;

public class Item {

    // =========================
    // Fields (mapping với bảng products)
    // =========================
    private String productId;

    private String productName;

    private String description;

    private double startingPrice;

    private double stepPrice;

    // seller_id -> object Seller
    private Seller seller;

    // ACTIVE / SOLD / CLOSED
    private String status;

    private Timestamp createdAt;

    // =========================
    // Constructor rỗng
    // =========================
    public Item() {
    }

    // =========================
    // Constructor đầy đủ
    // =========================
    public Item(String productId,
                String productName,
                String description,
                double startingPrice,
                double stepPrice,
                Seller seller,
                String status,
                Timestamp createdAt) {

        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.stepPrice = stepPrice;
        this.seller = seller;
        this.status = status;
        this.createdAt = createdAt;
    }

    // =========================
    // Getter & Setter
    // =========================

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    // -------------------------

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    // -------------------------

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // -------------------------

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    // -------------------------

    public double getStepPrice() {
        return stepPrice;
    }

    public void setStepPrice(double stepPrice) {
        this.stepPrice = stepPrice;
    }

    // -------------------------

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    // -------------------------

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // -------------------------

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // =========================
    // Business Methods
    // =========================

    // Kiểm tra sản phẩm còn hoạt động không
    public boolean isActive() {

        return status.equalsIgnoreCase("ACTIVE");
    }

    // Đóng đấu giá
    public void closeAuction() {

        this.status = "CLOSED";
    }

    // Đánh dấu đã bán
    public void soldItem() {

        this.status = "SOLD";
    }

    // =========================
    // toString()
    // =========================
    @Override
    public String toString() {

        return "Item{" +
                "productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", description='" + description + '\'' +
                ", startingPrice=" + startingPrice +
                ", stepPrice=" + stepPrice +
                ", seller=" +
                (seller != null ? seller.getUsername() : "null") +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}