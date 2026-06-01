package org.example.dto.response;

import org.example.model.Auction;
import org.example.model.enums.AuctionStatus;
import java.sql.Timestamp;

/**
 * DTO for displaying auction bidding information.
 * Contains only fields related to the auction process (bids, status, time).
 */
public class AuctionResponse {
    private int auctionId;
    private int productId;
    private long startingPrice;
    private long currentPrice;
    private long stepPrice;
    private Long buyNowPrice;
    private String sellerAccountname;
    private String winnerAccountname;
    private AuctionStatus status;
    private Timestamp startTime;
    private Timestamp endTime;

    public AuctionResponse() {}

    public AuctionResponse(Auction auction) {
        if (auction == null) return;
        this.auctionId = auction.getAuctionId();
        this.productId = auction.getProductId();
        this.startingPrice = auction.getStartingPrice();
        this.currentPrice = auction.getCurrentPrice();
        this.stepPrice = auction.getStepPrice();
        this.buyNowPrice = auction.getBuyNowPrice();
        this.sellerAccountname = auction.getSellerAccountname();
        this.winnerAccountname = auction.getWinnerAccountname();
        this.status = auction.getStatus();
        this.startTime = auction.getStartTime();
        this.endTime = auction.getEndTime();
    }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }

    public long getStepPrice() { return stepPrice; }
    public void setStepPrice(long stepPrice) { this.stepPrice = stepPrice; }

    public Long getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(Long buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    public String getSellerAccountname() { return sellerAccountname; }
    public void setSellerAccountname(String sellerAccountname) { this.sellerAccountname = sellerAccountname; }

    public String getWinnerAccountname() { return winnerAccountname; }
    public void setWinnerAccountname(String winnerAccountname) { this.winnerAccountname = winnerAccountname; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
}
