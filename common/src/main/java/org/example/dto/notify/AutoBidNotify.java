package org.example.dto.notify;

public class AutoBidNotify {
    private int auctionId;
    private String bidderAccountname;

    public AutoBidNotify() {}

    public AutoBidNotify(int auctionId, String bidderAccountname) {
        this.auctionId = auctionId;
        this.bidderAccountname = bidderAccountname;
    }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public String getBidderAccountname() { return bidderAccountname; }
    public void setBidderAccountname(String bidderAccountname) { this.bidderAccountname = bidderAccountname; }
}
