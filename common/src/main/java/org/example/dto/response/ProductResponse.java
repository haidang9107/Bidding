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

    /**
     * Default constructor for ProductResponse.
     */
    public ProductResponse() {}

    /**
     * Constructs a ProductResponse from an Item object.
     * @param item the item to convert
     */
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
    /**
     * Gets the product ID.
     * @return the product ID
     */
    public int getProductId() { return productId; }

    /**
     * Sets the product ID.
     * @param productId the product ID to set
     */
    public void setProductId(int productId) { this.productId = productId; }

    /**
     * Gets the auction ID.
     * @return the auction ID
     */
    public int getAuctionId() { return auctionId; }

    /**
     * Sets the auction ID.
     * @param auctionId the auction ID to set
     */
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    /**
     * Gets the product name.
     * @return the name
     */
    public String getName() { return name; }

    /**
     * Sets the product name.
     * @param name the name to set
     */
    public void setName(String name) { this.name = name; }

    /**
     * Gets the product description.
     * @return the description
     */
    public String getDescription() { return description; }

    /**
     * Sets the product description.
     * @param description the description to set
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Gets the image URL.
     * @return the image URL
     */
    public String getImageUrl() { return imageUrl; }

    /**
     * Sets the image URL.
     * @param imageUrl the image URL to set
     */
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    /**
     * Gets the owner's account name.
     * @return the owner's account name
     */
    public String getOwnerAccountname() { return ownerAccountname; }

    /**
     * Sets the owner's account name.
     * @param ownerAccountname the owner's account name to set
     */
    public void setOwnerAccountname(String ownerAccountname) { this.ownerAccountname = ownerAccountname; }

    /**
     * Gets the starting price.
     * @return the starting price
     */
    public long getStartingPrice() { return startingPrice; }

    /**
     * Sets the starting price.
     * @param startingPrice the starting price to set
     */
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    /**
     * Gets the current price.
     * @return the current price
     */
    public long getCurrentPrice() { return currentPrice; }

    /**
     * Sets the current price.
     * @param currentPrice the current price to set
     */
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }

    /**
     * Gets the step price.
     * @return the step price
     */
    public long getStepPrice() { return stepPrice; }

    /**
     * Sets the step price.
     * @param stepPrice the step price to set
     */
    public void setStepPrice(long stepPrice) { this.stepPrice = stepPrice; }

    /**
     * Gets the buy now price.
     * @return the buy now price
     */
    public Long getBuyNowPrice() { return buyNowPrice; }

    /**
     * Sets the buy now price.
     * @param buyNowPrice the buy now price to set
     */
    public void setBuyNowPrice(Long buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    /**
     * Gets the seller's account name.
     * @return the seller's account name
     */
    public String getSellerAccountname() { return sellerAccountname; }

    /**
     * Sets the seller's account name.
     * @param sellerAccountname the seller's account name to set
     */
    public void setSellerAccountname(String sellerAccountname) { this.sellerAccountname = sellerAccountname; }

    /**
     * Gets the winner's account name.
     * @return the winner's account name
     */
    public String getWinnerAccountname() { return winnerAccountname; }

    /**
     * Sets the winner's account name.
     * @param winnerAccountname the winner's account name to set
     */
    public void setWinnerAccountname(String winnerAccountname) { this.winnerAccountname = winnerAccountname; }

    /**
     * Gets the item category.
     * @return the category
     */
    public ItemCategory getCategory() { return category; }

    /**
     * Sets the item category.
     * @param category the category to set
     */
    public void setCategory(ItemCategory category) { this.category = category; }

    /**
     * Gets the auction status.
     * @return the status
     */
    public AuctionStatus getStatus() { return status; }

    /**
     * Sets the auction status.
     * @param status the status to set
     */
    public void setStatus(AuctionStatus status) { this.status = status; }

    /**
     * Gets the auction start time.
     * @return the start time
     */
    public Timestamp getStartTime() { return startTime; }

    /**
     * Sets the auction start time.
     * @param startTime the start time to set
     */
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    /**
     * Gets the auction end time.
     * @return the end time
     */
    public Timestamp getEndTime() { return endTime; }

    /**
     * Sets the auction end time.
     * @param endTime the end time to set
     */
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
}
