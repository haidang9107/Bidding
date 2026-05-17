package org.example.model;

import java.sql.Timestamp;

/**
 * Represents a bid placed by a bidder on an item.
 * This can be used as a DTO for real-time bidding messages.
 */
public class Bid {

    private int productId;
    private int bidderId;
    private String bidderName; // Optional: for display purposes
    private long bidAmount;
    private Timestamp bidTime;

    public Bid() {
    }

    public Bid(int productId, int bidderId, String bidderName, long bidAmount, Timestamp bidTime) {
        this.productId = productId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    // Getters and Setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getBidderId() { return bidderId; }
    public void setBidderId(int bidderId) { this.bidderId = bidderId; }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }

    public long getBidAmount() { return bidAmount; }
    public void setBidAmount(long bidAmount) { this.bidAmount = bidAmount; }

    public Timestamp getBidTime() { return bidTime; }
    public void setBidTime(Timestamp bidTime) { this.bidTime = bidTime; }
}
