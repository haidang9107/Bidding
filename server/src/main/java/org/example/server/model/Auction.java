package org.example.server.model;

import java.sql.Timestamp;

public class Auction {

    // =========================
    // Fields (mapping với bảng auctions)
    // =========================
    private String auctionId;

    // Product được đấu giá
    private Item item;

    // Người đấu giá
    private Bidder bidder;

    // Giá bid
    private double bidAmount;

    // Thời gian bid
    private Timestamp bidTime;

    // =========================
    // Constructor rỗng
    // =========================
    public Auction() {
    }

    // =========================
    // Constructor đầy đủ
    // =========================
    public Auction(String auctionId,
                   Item item,
                   Bidder bidder,
                   double bidAmount,
                   Timestamp bidTime) {

        this.auctionId = auctionId;
        this.item = item;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    // =========================
    // Getter & Setter
    // =========================

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    // -------------------------

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    // -------------------------

    public Bidder getBidder() {
        return bidder;
    }

    public void setBidder(Bidder bidder) {
        this.bidder = bidder;
    }

    // -------------------------

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    // -------------------------

    public Timestamp getBidTime() {
        return bidTime;
    }

    public void setBidTime(Timestamp bidTime) {
        this.bidTime = bidTime;
    }

    // =========================
    // Business Methods
    // =========================

    // Kiểm tra bid hợp lệ
    public boolean isValidBid() {

        return bidAmount >
                item.getStartingPrice();
    }

    // Hiển thị thông tin bid
    public void displayBidInfo() {

        System.out.println(
                bidder.getUsername() +
                        " bid $" +
                        bidAmount +
                        " on " +
                        item.getProductName()
        );
    }

    // =========================
    // toString()
    // =========================
    @Override
    public String toString() {

        return "Auction{" +
                "auctionId='" + auctionId + '\'' +
                ", item=" +
                (item != null
                        ? item.getProductName()
                        : "null") +
                ", bidder=" +
                (bidder != null
                        ? bidder.getUsername()
                        : "null") +
                ", bidAmount=" + bidAmount +
                ", bidTime=" + bidTime +
                '}';
    }
}