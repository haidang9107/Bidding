package org.example.model.product;

import org.example.model.enums.ItemCategory;
import org.example.model.enums.AuctionStatus;
import java.sql.Timestamp;

/**
 * Represents an electronic product.
 */
public class Electronics extends Item {

    private String brand;
    private int warrantyMonths;

    public Electronics() {
        super();
        this.setCategory(ItemCategory.ELECTRONICS);
    }

    public Electronics(int productId, String productName, String description, long startingPrice, 
                       long currentPrice, long stepPrice, int sellerId, Integer winnerId, 
                       AuctionStatus status, Timestamp startTime, Timestamp endTime, 
                       int version, Timestamp createdAt, String brand, int warrantyMonths) {
        super(productId, productName, description, startingPrice, currentPrice, stepPrice, 
              sellerId, winnerId, ItemCategory.ELECTRONICS, status, startTime, endTime, version, createdAt);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
}
