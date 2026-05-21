package org.example.dto;

/**
 * DTO for joining or leaving a realtime auction room.
 */
public class AuctionRoomRequest {
    private int auctionId;

    public AuctionRoomRequest() {}

    public AuctionRoomRequest(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }
}
