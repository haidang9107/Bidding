package org.example.dto;

/**
 * DTO for configuring automatic bidding on an auction.
 */
public class AutoBidRequest {
    private int auctionId;
    private long maxBid;
    private long incrementAmount;

    public AutoBidRequest() {}

    public AutoBidRequest(int auctionId, long maxBid, long incrementAmount) {
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.incrementAmount = incrementAmount;
    }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public long getMaxBid() { return maxBid; }
    public void setMaxBid(long maxBid) { this.maxBid = maxBid; }

    public long getIncrementAmount() { return incrementAmount; }
    public void setIncrementAmount(long incrementAmount) { this.incrementAmount = incrementAmount; }
}
