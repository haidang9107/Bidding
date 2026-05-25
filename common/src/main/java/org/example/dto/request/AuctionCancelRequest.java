package org.example.dto.request;

/**
 * DTO for an admin to cancel an ongoing auction.
 */
public class AuctionCancelRequest {
    private int auctionId;
    private String reason;

    /**
     * Default constructor for AuctionCancelRequest.
     */
    public AuctionCancelRequest() {}

    /**
     * Constructs an AuctionCancelRequest.
     * @param auctionId the ID of the auction to cancel
     * @param reason    the reason for cancellation
     */
    public AuctionCancelRequest(int auctionId, String reason) {
        this.auctionId = auctionId;
        this.reason = reason;
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
