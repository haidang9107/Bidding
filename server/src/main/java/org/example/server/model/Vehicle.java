package org.example.server.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Thực thể đại diện cho các sản phẩm là Phương tiện (Xe cộ).
 * Kế thừa từ Item.
 */
public class Vehicle extends Item {

    public Vehicle() {
        super();
        this.setCategory("VEHICLE"); // Gán mặc định category là VEHICLE
    }

    /**
     * Constructor đầy đủ để ánh xạ dữ liệu từ database
     */
    public Vehicle(String productId, String productName, String description, BigDecimal startingPrice,
                   BigDecimal stepPrice, String sellerId, String status, String brand,
                   int warrantyMonths, String artist, String artType, String model,
                   int manufactureYear, Timestamp createdAt) {

        // Gọi constructor của lớp cha (Item)
        super(productId, productName, description, startingPrice, stepPrice, sellerId,
                "VEHICLE", status, brand, warrantyMonths, artist, artType, model,
                manufactureYear, createdAt);
    }

    // Một logic nhỏ riêng cho Vehicle: tính tuổi thọ xe
    public int getVehicleAge(int currentYear) {
        return currentYear - this.getManufactureYear();
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "id='" + getProductId() + '\'' +
                ", name='" + getProductName() + '\'' +
                ", brand='" + getBrand() + '\'' +
                ", model='" + getModel() + '\'' +
                ", year=" + getManufactureYear() +
                ", price=" + getStartingPrice() +
                '}';
    }
}