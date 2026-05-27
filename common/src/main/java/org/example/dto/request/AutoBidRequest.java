package org.example.dto.request;

/**
 * DTO for configuring automatic bidding on an auction.
 */
public class AutoBidRequest {
    private int auctionId;
    private long maxBid;
    private long incrementAmount;

    /**
     * Default constructor for AutoBidRequest.
     */
    public AutoBidRequest() {}

    /**
     * Constructs an AutoBidRequest with specified configuration.
     * @param auctionId the ID of the auction
     * @param maxBid the maximum amount to bid automatically
     * @param incrementAmount the amount to increment the bid by
     */
    public AutoBidRequest(int auctionId, long maxBid, long incrementAmount) {
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.incrementAmount = incrementAmount;
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
     * Gets the maximum bid amount.
     * @return the max bid
     */
    public long getMaxBid() { return maxBid; }

    /**
     * Sets the maximum bid amount.
     * @param maxBid the max bid to set
     */
    public void setMaxBid(long maxBid) { this.maxBid = maxBid; }

    /**
     * Gets the increment amount.
     * @return the increment amount
     */
    public long getIncrementAmount() { return incrementAmount; }

    /**
     * Sets the increment amount.
     * @param incrementAmount the increment amount to set
     */
    public void setIncrementAmount(long incrementAmount) { this.incrementAmount = incrementAmount; }
}
