package org.example.dto.request;

public class BidRequest {
    private int auctionId;
    private int productId;
    private long amount;

    public BidRequest() {}
    public BidRequest(int productId, String bidderAccountname, long amount) {
        this.productId = productId;
        this.amount = amount;
    }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getBidderAccountname() { return null; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
}
