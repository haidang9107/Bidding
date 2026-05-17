package org.example.model.product;

import org.example.model.enums.ItemCategory;
import org.example.model.enums.AuctionStatus;
import java.sql.Timestamp;

/**
 * Represents a generic item in the auction system.
 * This class serves as a base for specific product types and handles auction timing.
 */
public abstract class Item {

    private int productId;
    private String productName;
    private String description;
    private long startingPrice;
    private long currentPrice;
    private long stepPrice;
    private int sellerId;
    private Integer winnerId; // Can be null
    private ItemCategory category;
    private AuctionStatus status;
    
    private Timestamp startTime;
    private Timestamp endTime;
    
    private int version; // For optimistic locking
    private Timestamp createdAt;

    public Item() {
    }

    public Item(int productId, String productName, String description, long startingPrice, 
                long currentPrice, long stepPrice, int sellerId, Integer winnerId, 
                ItemCategory category, AuctionStatus status, Timestamp startTime, 
                Timestamp endTime, int version, Timestamp createdAt) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.stepPrice = stepPrice;
        this.sellerId = sellerId;
        this.winnerId = winnerId;
        this.category = category;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.version = version;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }

    public long getStepPrice() { return stepPrice; }
    public void setStepPrice(long stepPrice) { this.stepPrice = stepPrice; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public Integer getWinnerId() { return winnerId; }
    public void setWinnerId(Integer winnerId) { this.winnerId = winnerId; }

    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
