package org.example.model;

import java.sql.Timestamp;

/**
 * Represents a single bid placed by a bidder in an auction.
 * Maps to a row in the {@code bids} table.
 */
public class Bid {

    private int auctionId;
    private String bidderAccountname;
    private long bidAmount;
    private Timestamp bidTime;

    /**
     * Default constructor for Bid.
     */
    public Bid() {
    }

    /**
     * Constructs a Bid with all fields.
     * @param auctionId         The ID of the auction this bid belongs to.
     * @param bidderAccountname The account name of the bidder.
     * @param bidAmount         The amount of the bid.
     * @param bidTime           The timestamp when the bid was placed.
     */
    public Bid(int auctionId, String bidderAccountname, long bidAmount, Timestamp bidTime) {
        this.auctionId = auctionId;
        this.bidderAccountname = bidderAccountname;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    // Getters and Setters
    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public String getBidderAccountname() { return bidderAccountname; }
    public void setBidderAccountname(String bidderAccountname) { this.bidderAccountname = bidderAccountname; }

    public long getBidAmount() { return bidAmount; }
    public void setBidAmount(long bidAmount) { this.bidAmount = bidAmount; }

    public Timestamp getBidTime() { return bidTime; }
    public void setBidTime(Timestamp bidTime) { this.bidTime = bidTime; }
}
