package org.example.dto.notify;

import org.example.dto.response.ProductResponse;

/**
 * Notification DTO for when an auction ends.
 */
public class AuctionEndNotify {
    private int auctionId;
    private String winnerAccountname;
    private long finalPrice;
    private String productName;
    private ProductResponse productDetail;

    /**
     * Default constructor for AuctionEndNotify.
     */
    public AuctionEndNotify() {}

    /**
     * Constructs an AuctionEndNotify with specified details.
     * @param auctionId the ID of the ended auction
     * @param winnerAccountname the account name of the auction winner
     * @param finalPrice the final price of the auctioned item
     * @param productName the name of the auctioned product
     * @param productDetail the detailed product information
     */
    public AuctionEndNotify(int auctionId, String winnerAccountname, long finalPrice, String productName, ProductResponse productDetail) {
        this.auctionId = auctionId;
        this.winnerAccountname = winnerAccountname;
        this.finalPrice = finalPrice;
        this.productName = productName;
        this.productDetail = productDetail;
    }

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
     * Gets the final price.
     * @return the final price
     */
    public long getFinalPrice() { return finalPrice; }

    /**
     * Sets the final price.
     * @param finalPrice the final price to set
     */
    public void setFinalPrice(long finalPrice) { this.finalPrice = finalPrice; }

    /**
     * Gets the product name.
     * @return the product name
     */
    public String getProductName() { return productName; }

    /**
     * Sets the product name.
     * @param productName the product name to set
     */
    public void setProductName(String productName) { this.productName = productName; }

    /**
     * Gets the product detail.
     * @return the product detail
     */
    public ProductResponse getProductDetail() { return productDetail; }

    /**
     * Sets the product detail.
     * @param productDetail the product detail to set
     */
    public void setProductDetail(ProductResponse productDetail) { this.productDetail = productDetail; }
}
