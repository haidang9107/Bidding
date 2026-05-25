package org.example.dto.response;

import org.example.model.Auction;
import org.example.model.enums.AuctionStatus;
import org.example.model.enums.ItemCategory;
import org.example.model.product.Product;

import java.sql.Timestamp;

/**
 * DTO for displaying product + auction information on the client side.
 *
 * <p>This is a composite view that combines fields from both the {@link Product}
 * and {@link Auction} domain models. The client typically consumes this as a
 * single object even though the server stores them separately.
 */
public class ProductResponse {
    // Product fields
    private int productId;
    private String name;
    private String description;
    private String imageUrl;
    private String ownerAccountname;
    private ItemCategory category;
    private boolean inAuction;

    // Auction fields
    private int auctionId;
    private long startingPrice;
    private long currentPrice;
    private long stepPrice;
    private Long buyNowPrice;
    private String sellerAccountname;
    private String winnerAccountname;
    private AuctionStatus status;
    private Timestamp startTime;
    private Timestamp endTime;

    /**
     * Default constructor for ProductResponse.
     */
    public ProductResponse() {}

    /**
     * Constructs a ProductResponse from an {@link Auction} (with its product loaded).
     * @param auction the auction whose product is populated
     */
    public ProductResponse(Auction auction) {
        if (auction == null) return;

        // Auction fields
        this.auctionId = auction.getAuctionId();
        this.startingPrice = auction.getStartingPrice();
        this.currentPrice = auction.getCurrentPrice();
        this.stepPrice = auction.getStepPrice();
        this.buyNowPrice = auction.getBuyNowPrice();
        this.sellerAccountname = auction.getSellerAccountname();
        this.winnerAccountname = auction.getWinnerAccountname();
        this.status = auction.getStatus();
        this.startTime = auction.getStartTime();
        this.endTime = auction.getEndTime();
        this.productId = auction.getProductId();

        // Product fields (if loaded)
        Product product = auction.getProduct();
        if (product != null) {
            this.productId = product.getProductId();
            this.name = product.getName();
            this.description = product.getDescription();
            this.imageUrl = product.getImageUrl();
            this.ownerAccountname = product.getOwnerAccountname();
            this.category = product.getCategory();
            this.inAuction = product.isInAuction();
        }
    }

    /**
     * Constructs a ProductResponse from a standalone {@link Product} (no auction context).
     * @param product the product
     */
    public ProductResponse(Product product) {
        if (product == null) return;
        this.productId = product.getProductId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.imageUrl = product.getImageUrl();
        this.ownerAccountname = product.getOwnerAccountname();
        this.category = product.getCategory();
        this.inAuction = product.isInAuction();
    }

    // Product getters/setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getOwnerAccountname() { return ownerAccountname; }
    public void setOwnerAccountname(String ownerAccountname) { this.ownerAccountname = ownerAccountname; }

    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public boolean isInAuction() { return inAuction; }
    public void setInAuction(boolean inAuction) { this.inAuction = inAuction; }

    // Auction getters/setters
    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

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
