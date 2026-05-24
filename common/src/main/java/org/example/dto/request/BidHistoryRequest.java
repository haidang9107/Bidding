package org.example.dto.request;

/**
 * Request for a paged list of bids for a specific auction.
 */
public class BidHistoryRequest extends PaginationRequest {
    private int auctionId;

    /**
     * Default constructor for BidHistoryRequest.
     */
    public BidHistoryRequest() {}

    /**
     * Constructs a BidHistoryRequest with auction ID and pagination details.
     * @param auctionId The ID of the auction.
     * @param page The page number.
     * @param pageSize The number of items per page.
     */
    public BidHistoryRequest(int auctionId, int page, int pageSize) {
        super(page, pageSize);
        this.auctionId = auctionId;
    }

    /**
     * Gets the auction ID.
     * @return The auction ID.
     */
    public int getAuctionId() { return auctionId; }

    /**
     * Sets the auction ID.
     * @param auctionId The auction ID to set.
     */
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }
}
