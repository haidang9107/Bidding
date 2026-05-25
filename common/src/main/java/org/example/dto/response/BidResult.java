package org.example.dto.response;

import java.sql.Timestamp;

/**
 * Result summary after a manual or automatic bid sequence.
 */
public class BidResult {
    private int auctionId;
    private String winnerAccountname;
    private long currentPrice;
    private boolean autoBidApplied;
    private Timestamp newEndTime;

    /**
     * Default constructor for BidResult.
     */
    public BidResult() {}

    /**
     * Constructs a BidResult without a new end time.
     * @param auctionId the ID of the auction
     * @param winnerAccountname the current winner's account name
     * @param currentPrice the current price of the auction
     * @param autoBidApplied whether an auto-bid was applied
     */
    public BidResult(int auctionId, String winnerAccountname, long currentPrice, boolean autoBidApplied) {
        this.auctionId = auctionId;
        this.winnerAccountname = winnerAccountname;
        this.currentPrice = currentPrice;
        this.autoBidApplied = autoBidApplied;
    }

    /**
     * Constructs a BidResult with a new end time.
     * @param auctionId the ID of the auction
     * @param winnerAccountname the current winner's account name
     * @param currentPrice the current price of the auction
     * @param autoBidApplied whether an auto-bid was applied
     * @param newEndTime the updated end time of the auction
     */
    public BidResult(int auctionId, String winnerAccountname, long currentPrice, boolean autoBidApplied, Timestamp newEndTime) {
        this.auctionId = auctionId;
        this.winnerAccountname = winnerAccountname;
        this.currentPrice = currentPrice;
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
     * Gets the winner's account name.
     * @return the winner's account name
     */
    public String getWinnerAccountname() { return winnerAccountname; }

    /**
     * Sets the winner's account name.
     * @param winnerAccountname the winner's account name to set
     */
    public void setWinnerAccountname(String winnerAccountname) { this.winnerAccountname = winnerAccountname; }

    /**
     * Gets the current price.
     * @return the current price
     */
    public long getCurrentPrice() { return currentPrice; }

    /**
     * Sets the current price.
     * @param currentPrice the current price to set
     */
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }

    /**
     * Checks if auto-bid was applied.
     * @return true if auto-bid was applied, false otherwise
     */
    public boolean isAutoBidApplied() { return autoBidApplied; }

    /**
     * Sets whether auto-bid was applied.
     * @param autoBidApplied true if auto-bid was applied, false otherwise
     */
    public void setAutoBidApplied(boolean autoBidApplied) { this.autoBidApplied = autoBidApplied; }

    /**
     * Gets the new end time.
     * @return the new end time
     */
    public Timestamp getNewEndTime() { return newEndTime; }

    /**
     * Sets the new end time.
     * @param newEndTime the new end time to set
     */
    public void setNewEndTime(Timestamp newEndTime) { this.newEndTime = newEndTime; }
}
