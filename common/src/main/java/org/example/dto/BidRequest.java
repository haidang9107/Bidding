package org.example.dto;

public class BidRequest {
    private int productId;
    private int bidderId;
    private long amount;

    public BidRequest() {}
    public BidRequest(int productId, int bidderId, long amount) {
        this.productId = productId;
        this.bidderId = bidderId;
        this.amount = amount;
    }

    public int getProductId() { return productId; }
    public int getBidderId() { return bidderId; }
    public long getAmount() { return amount; }
}
