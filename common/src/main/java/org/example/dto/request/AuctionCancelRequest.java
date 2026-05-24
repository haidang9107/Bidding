package org.example.dto.request;

/**
 * DTO for admin to cancel an ongoing auction.
 */
public class AuctionCancelRequest {
    private int auctionId;
    private int productId;
    private String reason;

    /**
     * Default constructor for AuctionCancelRequest.
     */
    public AuctionCancelRequest() {}

    /**
     * Constructs an AuctionCancelRequest with specified product ID and reason.
     * @param productId the ID of the product whose auction is to be cancelled
     * @param reason the reason for cancellation
     */
    public AuctionCancelRequest(int productId, String reason) {
        this.productId = productId;
        this.reason = reason;
    }

    /**
     * Gets the auction ID.
     * @return the auction ID
     */
    public int getAuctionId() { return auctionId > 0 ? auctionId : productId; }

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
     * Gets the cancellation reason.
     * @return the reason
     */
    public String getReason() { return reason; }

    /**
     * Sets the cancellation reason.
     * @param reason the reason to set
     */
    public void setReason(String reason) { this.reason = reason; }
}
