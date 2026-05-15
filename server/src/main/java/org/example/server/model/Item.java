package org.example.server.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Thực thể đại diện cho Sản phẩm (Item/Product) trong hệ thống đấu giá.
 * Kế thừa từ Entity để lấy thuộc tính createdAt.
 */
public class Item extends Entity {
    private String productId;
    private String productName;
    private String description;
    private BigDecimal startingPrice;
    private BigDecimal stepPrice;
    private String sellerId; // Liên kết tới User (Seller)
    private String category;
    private String status;
    private String brand;
    private int warrantyMonths;
    private String artist;
    private String artType;
    private String model;
    private int manufactureYear;

    public Item() {
        super();
    }

    // Constructor đầy đủ để lấy dữ liệu từ database
    public Item(String productId, String productName, String description, BigDecimal startingPrice,
                BigDecimal stepPrice, String sellerId, String category, String status,
                String brand, int warrantyMonths, String artist, String artType,
                String model, int manufactureYear, Timestamp createdAt) {
        super(createdAt);
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.stepPrice = stepPrice;
        this.sellerId = sellerId;
        this.category = category;
        this.status = status;
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
        this.artist = artist;
        this.artType = artType;
        this.model = model;
        this.manufactureYear = manufactureYear;
    }

    // --- Getter và Setter ---

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

    public BigDecimal getStepPrice() { return stepPrice; }
    public void setStepPrice(BigDecimal stepPrice) { this.stepPrice = stepPrice; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getArtType() { return artType; }
    public void setArtType(String artType) { this.artType = artType; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getManufactureYear() { return manufactureYear; }
    public void setManufactureYear(int manufactureYear) { this.manufactureYear = manufactureYear; }

    @Override
    public String toString() {
        return "Item{" +
                "productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", startingPrice=" + startingPrice +
                ", status='" + status + '\'' +
                ", sellerId='" + sellerId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}