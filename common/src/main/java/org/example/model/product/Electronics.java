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

    public Electronics(int productId, String name, String description, String imageUrl, 
                       long startingPrice, long currentPrice, long stepPrice, 
                       String sellerAccountname, String winnerAccountname, 
                       AuctionStatus status, Timestamp startTime, Timestamp endTime, 
                       int version, String brand, int warrantyMonths) {
        super(productId, name, description, imageUrl, startingPrice, currentPrice, stepPrice, 
              sellerAccountname, winnerAccountname, ItemCategory.ELECTRONICS, status, 
              startTime, endTime, version);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
}
