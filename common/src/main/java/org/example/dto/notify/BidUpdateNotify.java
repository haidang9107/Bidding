package org.example.dto.notify;

import java.sql.Timestamp;

/**
 * Notification DTO for bid updates in an auction.
 */
public class BidUpdateNotify {
    private int auctionId;
    private String bidderAccountname;
    private long amount;
    private boolean autoBidApplied;
    private Timestamp newEndTime;

    /**
     * Default constructor for BidUpdateNotify.
     */
    public BidUpdateNotify() {}

    /**
     * Constructs a BidUpdateNotify with the specified details.
     * @param auctionId the ID of the auction
     * @param bidderAccountname the account name of the bidder
     * @param amount the bid amount
     * @param autoBidApplied whether the bid was applied via auto-bid
     * @param newEndTime the updated end time of the auction, if applicable
     */
    public BidUpdateNotify(int auctionId, String bidderAccountname, long amount, boolean autoBidApplied, Timestamp newEndTime) {
        this.auctionId = auctionId;
        this.bidderAccountname = bidderAccountname;
        this.amount = amount;
        this.autoBidApplied = autoBidApplied;
        this.newEndTime = newEndTime;
    }

    /**
     * Gets the auction ID.
     * @return the auction ID
     */
    public int getAuctionId() { return auctionId; }

    /**
     * Sets the auction ID.
     * @param auctionId the auction ID to set
     */
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    /**
     * Gets the bidder's account name.
     * @return the bidder's account name
     */
    public String getBidderAccountname() { return bidderAccountname; }

    /**
     * Sets the bidder's account name.
     * @param bidderAccountname the bidder's account name to set
     */
    public void setBidderAccountname(String bidderAccountname) { this.bidderAccountname = bidderAccountname; }

    /**
     * Gets the bid amount.
     * @return the bid amount
     */
    public long getAmount() { return amount; }

    /**
     * Sets the bid amount.
     * @param amount the bid amount to set
     */
    public void setAmount(long amount) { this.amount = amount; }

    /**
     * Checks if the bid was applied via auto-bid.
     * @return true if applied via auto-bid, false otherwise
     */
    public boolean isAutoBidApplied() { return autoBidApplied; }

    /**
     * Sets whether the bid was applied via auto-bid.
     * @param autoBidApplied true if applied via auto-bid, false otherwise
     */
    public void setAutoBidApplied(boolean autoBidApplied) { this.autoBidApplied = autoBidApplied; }

    /**
     * Gets the new end time of the auction.
     * @return the new end time
     */
    public Timestamp getNewEndTime() { return newEndTime; }

    /**
     * Sets the new end time of the auction.
     * @param newEndTime the new end time to set
     */
    public void setNewEndTime(Timestamp newEndTime) { this.newEndTime = newEndTime; }
}
