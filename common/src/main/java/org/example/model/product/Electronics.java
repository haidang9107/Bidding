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

    /**
     * Default constructor for Electronics.
     */
    public Electronics() {
        super();
        this.setCategory(ItemCategory.ELECTRONICS);
    }

    /**
     * Constructs an Electronics item with all fields.
     * @param productId The unique product ID.
     * @param name The item name.
     * @param description The item description.
     * @param imageUrl The URL for the item's image.
     * @param startingPrice The initial price.
     * @param currentPrice The current highest bid.
     * @param stepPrice The minimum bid increment.
     * @param sellerAccountname The account name of the seller.
     * @param winnerAccountname The account name of the current winner.
     * @param status The auction status.
     * @param startTime The auction start time.
     * @param endTime The auction end time.
     * @param version The version for optimistic locking.
     * @param brand The brand of the electronic product.
     * @param warrantyMonths The warranty duration in months.
     */
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
