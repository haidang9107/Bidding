package org.example.dto.response;

import org.example.model.enums.AuctionStatus;
import org.example.model.enums.ItemCategory;
import org.example.model.product.Item;

import java.sql.Timestamp;

/**
 * DTO for displaying product information in a list or grid on the client side.
 */
public class ProductResponse {
    private int productId;
    private int auctionId;
    private String name;
    private String description;
    private String imageUrl;
    private String ownerAccountname;
    private long startingPrice;
    private long currentPrice;
    private long stepPrice;
    private Long buyNowPrice;
    private String sellerAccountname;
    private String winnerAccountname;
    private ItemCategory category;
    private AuctionStatus status;
    private Timestamp startTime;
    private Timestamp endTime;

    public ProductResponse() {}

    public ProductResponse(Item item) {
        this.productId = item.getProductId();
        this.auctionId = item.getAuctionId();
        this.name = item.getName();
        this.description = item.getDescription();
        this.imageUrl = item.getImageUrl();
        this.ownerAccountname = item.getOwnerAccountname();
        this.startingPrice = item.getStartingPrice();
        this.currentPrice = item.getCurrentPrice();
        this.stepPrice = item.getStepPrice();
        this.buyNowPrice = item.getBuyNowPrice();
        this.sellerAccountname = item.getSellerAccountname();
        this.winnerAccountname = item.getWinnerAccountname();
        this.category = item.getCategory();
        this.status = item.getStatus();
        this.startTime = item.getStartTime();
        this.endTime = item.getEndTime();
    }

    // Getters and Setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getOwnerAccountname() { return ownerAccountname; }
    public void setOwnerAccountname(String ownerAccountname) { this.ownerAccountname = ownerAccountname; }

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

    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
}
