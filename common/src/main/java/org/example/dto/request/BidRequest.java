package org.example.dto.request;

/**
 * Data Transfer Object for placing a bid on an auction.
 */
public class BidRequest {
    private int auctionId;
    private long amount;

    /**
     * Default constructor for BidRequest.
     */
    public BidRequest() {}

    /**
     * Constructs a BidRequest.
     * @param auctionId the ID of the auction being bid on
     * @param amount    the bid amount
     */
    public BidRequest(int auctionId, long amount) {
        this.auctionId = auctionId;
        this.amount = amount;
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
     * Gets the bid amount.
     * @return the bid amount
     */
    public long getAmount() { return amount; }

    /**
     * Sets the bid amount.
     * @param amount the bid amount to set
     */
    public void setAmount(long amount) { this.amount = amount; }
}
