package org.example.dto.notify;

import java.sql.Timestamp;

public class BidUpdateNotify {
    private int auctionId;
    private String bidderAccountname;
    private long amount;
    private boolean autoBidApplied;
    private Timestamp newEndTime;

    public BidUpdateNotify() {}

    public BidUpdateNotify(int auctionId, String bidderAccountname, long amount, boolean autoBidApplied, Timestamp newEndTime) {
        this.auctionId = auctionId;
        this.bidderAccountname = bidderAccountname;
        this.amount = amount;
        this.autoBidApplied = autoBidApplied;
        this.newEndTime = newEndTime;
    }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public String getBidderAccountname() { return bidderAccountname; }
    public void setBidderAccountname(String bidderAccountname) { this.bidderAccountname = bidderAccountname; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public boolean isAutoBidApplied() { return autoBidApplied; }
    public void setAutoBidApplied(boolean autoBidApplied) { this.autoBidApplied = autoBidApplied; }

    public Timestamp getNewEndTime() { return newEndTime; }
    public void setNewEndTime(Timestamp newEndTime) { this.newEndTime = newEndTime; }
}
