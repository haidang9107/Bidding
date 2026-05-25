package org.example.model;

import org.example.model.enums.AuctionStatus;
import org.example.model.product.Product;

import java.sql.Timestamp;

/**
 * Represents an auction session for a product.
 * A product can have multiple Auction records over time (1-to-many).
 *
 * <p>The associated {@link Product} may be eagerly loaded (when the auction is
 * fetched through a JOIN query) or left null when only the auction row is needed.
 */
public class Auction {

    private int auctionId;
    private int productId;
    private String sellerAccountname;
    private String winnerAccountname; // null if no bids yet
    private long startingPrice;
    private long currentPrice;
    private long stepPrice;
    private Long buyNowPrice;         // null if buy-now disabled
    private Timestamp startTime;
    private Timestamp endTime;
    private AuctionStatus status;
    private int version;              // For optimistic locking (legacy, kept for compatibility)

    /** The product being auctioned. May be null if not loaded. */
    private Product product;

    /**
     * Default constructor.
     */
    public Auction() {
    }

    /**
     * Constructs an Auction with the core auction fields.
     * @param auctionId         The unique auction ID.
     * @param productId         The product ID this auction belongs to.
     * @param sellerAccountname The seller's account name.
     * @param winnerAccountname The current leading bidder, or null.
     * @param startingPrice     The starting price.
     * @param currentPrice      The current highest bid.
     * @param stepPrice         The minimum bid increment.
     * @param buyNowPrice       The buy-now price (nullable).
     * @param startTime         The start time.
     * @param endTime           The end time.
     * @param status            The auction status.
     * @param version           The optimistic-lock version.
     */
    public Auction(int auctionId, int productId, String sellerAccountname,
                   String winnerAccountname, long startingPrice, long currentPrice,
                   long stepPrice, Long buyNowPrice, Timestamp startTime,
                   Timestamp endTime, AuctionStatus status, int version) {
        this.auctionId = auctionId;
        this.productId = productId;
        this.sellerAccountname = sellerAccountname;
        this.winnerAccountname = winnerAccountname;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.stepPrice = stepPrice;
        this.buyNowPrice = buyNowPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.version = version;
    }

    // Getters and Setters
    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getSellerAccountname() { return sellerAccountname; }
    public void setSellerAccountname(String sellerAccountname) { this.sellerAccountname = sellerAccountname; }

    public String getWinnerAccountname() { return winnerAccountname; }
    public void setWinnerAccountname(String winnerAccountname) { this.winnerAccountname = winnerAccountname; }

    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }

    public long getStepPrice() { return stepPrice; }
    public void setStepPrice(long stepPrice) { this.stepPrice = stepPrice; }

    public Long getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(Long buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
