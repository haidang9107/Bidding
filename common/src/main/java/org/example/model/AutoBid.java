package org.example.model;

import java.sql.Timestamp;

/**
 * Represents an automatic bidding configuration for one auction and bidder.
 */
public class AutoBid {
    private int autoBidId;
    private int auctionId;
    private String bidderAccountname;
    private long maxBid;
    private long incrementAmount;
    private boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public AutoBid() {}

    public AutoBid(int autoBidId, int auctionId, String bidderAccountname, long maxBid,
                   long incrementAmount, boolean active, Timestamp createdAt, Timestamp updatedAt) {
        this.autoBidId = autoBidId;
        this.auctionId = auctionId;
        this.bidderAccountname = bidderAccountname;
        this.maxBid = maxBid;
        this.incrementAmount = incrementAmount;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getAutoBidId() { return autoBidId; }
    public int getAuctionId() { return auctionId; }
    public String getBidderAccountname() { return bidderAccountname; }
    public long getMaxBid() { return maxBid; }
    public long getIncrementAmount() { return incrementAmount; }
    public boolean isActive() { return active; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
}
