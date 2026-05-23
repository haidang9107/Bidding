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

    public BidResult() {}

    public BidResult(int auctionId, String winnerAccountname, long currentPrice, boolean autoBidApplied) {
        this.auctionId = auctionId;
        this.winnerAccountname = winnerAccountname;
        this.currentPrice = currentPrice;
        this.autoBidApplied = autoBidApplied;
    }

    public BidResult(int auctionId, String winnerAccountname, long currentPrice, boolean autoBidApplied, Timestamp newEndTime) {
        this.auctionId = auctionId;
        this.winnerAccountname = winnerAccountname;
        this.currentPrice = currentPrice;
        this.autoBidApplied = autoBidApplied;
        this.newEndTime = newEndTime;
    }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public String getWinnerAccountname() { return winnerAccountname; }
    public void setWinnerAccountname(String winnerAccountname) { this.winnerAccountname = winnerAccountname; }

    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }

    public boolean isAutoBidApplied() { return autoBidApplied; }
    public void setAutoBidApplied(boolean autoBidApplied) { this.autoBidApplied = autoBidApplied; }

    public Timestamp getNewEndTime() { return newEndTime; }
    public void setNewEndTime(Timestamp newEndTime) { this.newEndTime = newEndTime; }
}
