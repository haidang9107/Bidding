package org.example.dto.request;

/**
 * DTO for joining or leaving a realtime auction room.
 */
public class AuctionRoomRequest {
    private int auctionId;

    /**
     * Default constructor for AuctionRoomRequest.
     */
    public AuctionRoomRequest() {}

    /**
     * Constructs an AuctionRoomRequest with specified auction ID.
     * @param auctionId the ID of the auction room
     */
    public AuctionRoomRequest(int auctionId) {
        this.auctionId = auctionId;
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
}
