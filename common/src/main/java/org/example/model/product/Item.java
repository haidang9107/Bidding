package org.example.model.product;

import org.example.model.enums.ItemCategory;
import org.example.model.enums.AuctionStatus;
import java.sql.Timestamp;

/**
 * Represents a generic item in the auction system.
 * Simplified: Uses sellerAccountname and winnerAccountname (Strings).
 */
public abstract class Item {

    private int productId;
    private int auctionId;
    private String name;
    private String description;
    private String imageUrl;
    private String ownerAccountname;
    private boolean inAuction;
    private Timestamp withdrawnAt;
    private long startingPrice;
    private long currentPrice;
    private long stepPrice;
    private Long buyNowPrice;
    private String sellerAccountname;
    private String winnerAccountname; // Can be null
    private ItemCategory category;
    private AuctionStatus status;
    
    private Timestamp startTime;
    private Timestamp endTime;
    
    private int version; // For optimistic locking

    /**
     * Default constructor for Item.
     */
    public Item() {
    }

    /**
     * Constructs an Item with all fields.
     * @param productId The unique product ID.
     * @param name The item name.
     * @param description The item description.
     * @param imageUrl The URL for the item's image.
     * @param startingPrice The initial price.
     * @param currentPrice The current highest bid.
     * @param stepPrice The minimum bid increment.
     * @param sellerAccountname The account name of the seller.
     * @param winnerAccountname The account name of the current winner.
     * @param category The item category.
     * @param status The auction status.
     * @param startTime The auction start time.
     * @param endTime The auction end time.
     * @param version The version for optimistic locking.
     */
    public Item(int productId, String name, String description, String imageUrl, 
                long startingPrice, long currentPrice, long stepPrice, 
                String sellerAccountname, String winnerAccountname, 
                ItemCategory category, AuctionStatus status, Timestamp startTime, 
                Timestamp endTime, int version) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.stepPrice = stepPrice;
        this.sellerAccountname = sellerAccountname;
        this.winnerAccountname = winnerAccountname;
        this.category = category;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.version = version;
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

    public boolean isInAuction() { return inAuction; }
    public void setInAuction(boolean inAuction) { this.inAuction = inAuction; }

    public Timestamp getWithdrawnAt() { return withdrawnAt; }
    public void setWithdrawnAt(Timestamp withdrawnAt) { this.withdrawnAt = withdrawnAt; }

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

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
