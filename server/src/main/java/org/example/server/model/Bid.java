package org.example.server.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Thực thể đại diện cho một lệnh đặt giá (Bid).
 * Trong một số kiến trúc, Bid có thể được coi là một bản ghi trong Auction.
 */
public class Bid extends Entity {
    private String bidId;
    private String bidderId;
    private String productId;
    private BigDecimal amount;

    public Bid() {
        super();
    }

    /**
     * Constructor đầy đủ
     * @param createdAt tương ứng với thời điểm đặt giá (bid_time)
     */
    public Bid(String bidId, String bidderId, String productId, BigDecimal amount, Timestamp createdAt) {
        super(createdAt);
        this.bidId = bidId;
        this.bidderId = bidderId;
        this.productId = productId;
        this.amount = amount;
    }

    // --- Getter và Setter ---

    public String getBidId() {
        return bidId;
    }

    public void setBidId(String bidId) {
        this.bidId = bidId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Bid{" +
                "bidId='" + bidId + '\'' +
                ", bidderId='" + bidderId + '\'' +
                ", amount=" + amount +
                ", time=" + createdAt +
                '}';
    }
}