package org.example.server.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Thực thể đại diện cho sản phẩm Đồ điện tử.
 * Kế thừa từ Item để sử dụng các thuộc tính chung của sản phẩm.
 */
public class Electronics extends Item {

    public Electronics() {
        super();
        this.setCategory("ELECTRONICS"); // Gán mặc định category là ELECTRONICS
    }

    /**
     * Constructor đầy đủ để ánh xạ dữ liệu từ MySQL
     */
    public Electronics(String productId, String productName, String description, BigDecimal startingPrice,
                       BigDecimal stepPrice, String sellerId, String status, String brand,
                       int warrantyMonths, String artist, String artType, String model,
                       int manufactureYear, Timestamp createdAt) {

        // Gọi constructor cha (Item)
        super(productId, productName, description, startingPrice, stepPrice, sellerId,
                "ELECTRONICS", status, brand, warrantyMonths, artist, artType, model,
                manufactureYear, createdAt);
    }

    // Phương thức kiểm tra tình trạng bảo hành (Logic ví dụ cho đồ điện tử)
    public boolean hasLongWarranty() {
        return this.getWarrantyMonths() > 12;
    }

    @Override
    public String toString() {
        return "Electronics{" +
                "id='" + getProductId() + '\'' +
                ", name='" + getProductName() + '\'' +
                ", brand='" + getBrand() + '\'' +
                ", model='" + getModel() + '\'' +
                ", warranty=" + getWarrantyMonths() + " months" +
                ", price=" + getStartingPrice() +
                '}';
    }
}