package org.example.dto;

public class BidRequest {
    private int auctionId;
    private int productId;
    private long amount;

    public BidRequest() {}
    public BidRequest(int productId, String bidderAccountname, long amount) {
        this.productId = productId;
        this.amount = amount;
    }

    public int getAuctionId() { return auctionId > 0 ? auctionId : productId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getProductId() { return productId; }
    public String getBidderAccountname() { return null; }
    public long getAmount() { return amount; }
}
