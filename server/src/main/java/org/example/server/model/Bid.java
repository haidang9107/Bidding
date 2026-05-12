package org.example.server.model;

import java.sql.Timestamp;

public class Bid {

    // =========================
    // Fields
    // =========================
    private String bidId;

    // Người đấu giá
    private Bidder bidder;

    // Sản phẩm được bid
    private Item item;

    // Giá bid
    private double bidAmount;

    // Thời gian bid
    private Timestamp bidTime;

    // =========================
    // Constructor rỗng
    // =========================
    public Bid() {
    }

    // =========================
    // Constructor đầy đủ
    // =========================
    public Bid(String bidId,
               Bidder bidder,
               Item item,
               double bidAmount,
               Timestamp bidTime) {

        this.bidId = bidId;
        this.bidder = bidder;
        this.item = item;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    // =========================
    // Getter & Setter
    // =========================

    public String getBidId() {
        return bidId;
    }

    public void setBidId(String bidId) {
        this.bidId = bidId;
    }

    // -------------------------

    public Bidder getBidder() {
        return bidder;
    }

    public void setBidder(Bidder bidder) {
        this.bidder = bidder;
    }

    // -------------------------

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
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

        if (item == null) {
            return false;
        }

        return bidAmount >=
                item.getStartingPrice()
                        + item.getStepPrice();
    }

    // Hiển thị thông tin bid
    public void displayBidInfo() {

        System.out.println(
                bidder.getUsername() +
                        " bid $" +
                        bidAmount +
                        " on product " +
                        item.getProductName()
        );
    }

    // =========================
    // toString()
    // =========================
    @Override
    public String toString() {

        return "Bid{" +
                "bidId='" + bidId + '\'' +
                ", bidder=" +
                (bidder != null
                        ? bidder.getUsername()
                        : "null") +
                ", item=" +
                (item != null
                        ? item.getProductName()
                        : "null") +
                ", bidAmount=" + bidAmount +
                ", bidTime=" + bidTime +
                '}';
    }
}