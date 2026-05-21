package org.example.dto;

/**
 * Result summary after a manual or automatic bid sequence.
 */
public class BidResult {
    private int auctionId;
    private String winnerAccountname;
    private long currentPrice;
    private boolean autoBidApplied;

    public BidResult() {}

    public BidResult(int auctionId, String winnerAccountname, long currentPrice, boolean autoBidApplied) {
        this.auctionId = auctionId;
        this.winnerAccountname = winnerAccountname;
        this.currentPrice = currentPrice;
        this.autoBidApplied = autoBidApplied;
    }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public String getWinnerAccountname() { return winnerAccountname; }
    public void setWinnerAccountname(String winnerAccountname) { this.winnerAccountname = winnerAccountname; }

    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }

    public boolean isAutoBidApplied() { return autoBidApplied; }
    public void setAutoBidApplied(boolean autoBidApplied) { this.autoBidApplied = autoBidApplied; }
}
