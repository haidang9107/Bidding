package org.example.model;

import java.sql.Timestamp;

/**
 * Represents an auction record (bid history) in the system.
 */
public class Auction {

    private int auctionId;
    private int productId;
    private int bidderId;
    private long bidAmount;
    private Timestamp bidTime;

    public Auction() {
    }

    public Auction(int auctionId, int productId, int bidderId, long bidAmount, Timestamp bidTime) {
        this.auctionId = auctionId;
        this.productId = productId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    // Getters and Setters
    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getBidderId() { return bidderId; }
    public void setBidderId(int bidderId) { this.bidderId = bidderId; }

    public long getBidAmount() { return bidAmount; }
    public void setBidAmount(long bidAmount) { this.bidAmount = bidAmount; }

    public Timestamp getBidTime() { return bidTime; }
    public void setBidTime(Timestamp bidTime) { this.bidTime = bidTime; }
}
