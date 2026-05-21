package org.example.model;

import java.sql.Timestamp;

/**
 * Represents an auction record (bid history) in the system.
 */
public class Auction {

    private int auctionId;
    private int productId;
    private String bidderAccountname;
    private long bidAmount;
    private Timestamp bidTime;

    public Auction() {
    }

    public Auction(int auctionId, int productId, String bidderAccountname, long bidAmount, Timestamp bidTime) {
        this.auctionId = auctionId;
        this.productId = productId;
        this.bidderAccountname = bidderAccountname;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    // Getters and Setters
    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getBidderAccountname() { return bidderAccountname; }
    public void setBidderAccountname(String bidderAccountname) { this.bidderAccountname = bidderAccountname; }

    public long getBidAmount() { return bidAmount; }
    public void setBidAmount(long bidAmount) { this.bidAmount = bidAmount; }

    public Timestamp getBidTime() { return bidTime; }
    public void setBidTime(Timestamp bidTime) { this.bidTime = bidTime; }
}
