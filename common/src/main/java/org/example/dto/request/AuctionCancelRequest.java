package org.example.dto.request;

/**
 * DTO for admin to cancel an ongoing auction.
 */
public class AuctionCancelRequest {
    private int auctionId;
    private int productId;
    private String reason;

    public AuctionCancelRequest() {}

    public AuctionCancelRequest(int productId, String reason) {
        this.productId = productId;
        this.reason = reason;
    }

    public int getAuctionId() { return auctionId > 0 ? auctionId : productId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
