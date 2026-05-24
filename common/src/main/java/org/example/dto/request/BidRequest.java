package org.example.dto.request;

/**
 * Data Transfer Object for placing a bid.
 */
public class BidRequest {
    private int auctionId;
    private int productId;
    private long amount;

    /**
     * Default constructor for BidRequest.
     */
    public BidRequest() {}

    /**
     * Constructs a BidRequest with specified product ID and amount.
     * @param productId the ID of the product being bid on
     * @param bidderAccountname the account name of the bidder (unused, handled by session)
     * @param amount the bid amount
     */
    public BidRequest(int productId, String bidderAccountname, long amount) {
        this.productId = productId;
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
     * Gets the product ID.
     * @return the product ID
     */
    public int getProductId() { return productId; }

    /**
     * Sets the product ID.
     * @param productId the product ID to set
     */
    public void setProductId(int productId) { this.productId = productId; }

    /**
     * Gets the bidder account name.
     * @return always null, as account name is retrieved from the session
     */
    public String getBidderAccountname() { return null; }

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
