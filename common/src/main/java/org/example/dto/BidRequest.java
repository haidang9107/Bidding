package org.example.dto;

public class BidRequest {
    private int productId;
    private String bidderAccountname;
    private long amount;

    public BidRequest() {}
    public BidRequest(int productId, String bidderAccountname, long amount) {
        this.productId = productId;
        this.bidderAccountname = bidderAccountname;
        this.amount = amount;
    }

    public int getProductId() { return productId; }
    public String getBidderAccountname() { return bidderAccountname; }
    public long getAmount() { return amount; }
}
