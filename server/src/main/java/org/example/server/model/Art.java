package org.example.server.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Thực thể đại diện cho sản phẩm Nghệ thuật.
 * Kế thừa từ Item, và gián tiếp kế thừa từ Entity.
 */
public class Art extends Item {

    public Art() {
        super();
        this.setCategory("ART"); // Gán mặc định category là ART
    }

    /**
     * Constructor đầy đủ để lấy dữ liệu từ database
     */
    public Art(String productId, String productName, String description, BigDecimal startingPrice,
               BigDecimal stepPrice, String sellerId, String status, String brand,
               int warrantyMonths, String artist, String artType, String model,
               int manufactureYear, Timestamp createdAt) {

        // Gọi constructor của lớp cha (Item)
        super(productId, productName, description, startingPrice, stepPrice, sellerId,
                "ART", status, brand, warrantyMonths, artist, artType, model,
                manufactureYear, createdAt);
    }

    // Các phương thức bổ sung riêng cho Art nếu cần (ví dụ: kiểm tra chứng nhận thật giả)
    public void displayArtInfo() {
        System.out.println("Tác phẩm: " + getProductName());
        System.out.println("Họa sĩ: " + getArtist());
        System.out.println("Thể loại: " + getArtType());
    }

    @Override
    public String toString() {
        return "Art{" +
                "id='" + getProductId() + '\'' +
                ", name='" + getProductName() + '\'' +
                ", artist='" + getArtist() + '\'' +
                ", type='" + getArtType() + '\'' +
                ", price=" + getStartingPrice() +
                '}';
    }
}