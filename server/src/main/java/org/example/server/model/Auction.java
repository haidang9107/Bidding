package org.example.server.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Thực thể đại diện cho một lượt trả giá (Auction/Bid) trong hệ thống.
 * Kế thừa từ Entity để quản lý thời gian đặt giá (bid_time).
 */
public class Auction extends Entity {
    private String auctionId;
    private String productId;
    private String bidderId;
    private BigDecimal bidAmount;

    public Auction() {
        super();
    }

    /**
     * Constructor đầy đủ để ánh xạ dữ liệu từ bảng auctions trong MySQL
     * @param bidTime tương ứng với trường bid_time trong database
     */
    public Auction(String auctionId, String productId, String bidderId,
                   BigDecimal bidAmount, Timestamp bidTime) {
        super(bidTime); // bid_time đóng vai trò là thời gian khởi tạo thực thể
        this.auctionId = auctionId;
        this.productId = productId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
    }

    // --- Getter và Setter ---

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }

    /**
     * Alias cho getCreatedAt() để code đọc thuận miệng hơn trong logic đấu giá
     */
    public Timestamp getBidTime() {
        return getCreatedAt();
    }

    public void setBidTime(Timestamp bidTime) {
        setCreatedAt(bidTime);
    }

    @Override
    public String toString() {
        return "Auction{" +
                "auctionId='" + auctionId + '\'' +
                ", productId='" + productId + '\'' +
                ", bidderId='" + bidderId + '\'' +
                ", bidAmount=" + bidAmount +
                ", bidTime=" + getCreatedAt() +
                '}';
    }
}