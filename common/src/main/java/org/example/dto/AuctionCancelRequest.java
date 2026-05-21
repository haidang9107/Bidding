package org.example.dto;

/**
 * DTO for admin to cancel an ongoing auction.
 */
public class AuctionCancelRequest {
    private int productId;
    private String reason;

    public AuctionCancelRequest() {}

    public AuctionCancelRequest(int productId, String reason) {
        this.productId = productId;
        this.reason = reason;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
