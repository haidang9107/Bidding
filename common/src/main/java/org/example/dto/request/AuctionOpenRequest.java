package org.example.dto.request;

import java.sql.Timestamp;

/**
 * DTO for opening an auction on an existing inventory product.
 *
 * <p>This is the second step of the seller flow: after creating the product
 * (via {@code PRODUCT_CREATE}) the seller chooses one of their inventory
 * items, fills in price + duration here, and submits this request to put
 * the item on the marketplace.
 */
public class AuctionOpenRequest {
    private int productId;
    private long startingPrice;
    private Long stepPrice;       // Optional: defaults to startingPrice / 10
    private Long buyNowPrice;     // Optional
    private Timestamp startTime;  // Optional: defaults to now
    private Timestamp endTime;    // Optional: defaults to now + duration

    public AuctionOpenRequest() {}

    public AuctionOpenRequest(int productId, long startingPrice) {
        this.productId = productId;
        this.startingPrice = startingPrice;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    public Long getStepPrice() { return stepPrice; }
    public void setStepPrice(Long stepPrice) { this.stepPrice = stepPrice; }

    public Long getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(Long buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
}
