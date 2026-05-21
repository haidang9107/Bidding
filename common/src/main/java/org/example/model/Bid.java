package org.example.model;

import java.sql.Timestamp;

/**
 * Represents a bid placed by a bidder on an item.
 * This can be used as a DTO for real-time bidding messages.
 */
public class Bid {

    private int productId;
    private String bidderAccountname;
    private long bidAmount;
    private Timestamp bidTime;

    public Bid() {
    }

    public Bid(int productId, String bidderAccountname, long bidAmount, Timestamp bidTime) {
        this.productId = productId;
        this.bidderAccountname = bidderAccountname;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    // Getters and Setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getBidderAccountname() { return bidderAccountname; }
    public void setBidderAccountname(String bidderAccountname) { this.bidderAccountname = bidderAccountname; }

    public long getBidAmount() { return bidAmount; }
    public void setBidAmount(long bidAmount) { this.bidAmount = bidAmount; }

    public Timestamp getBidTime() { return bidTime; }
    public void setBidTime(Timestamp bidTime) { this.bidTime = bidTime; }
}
