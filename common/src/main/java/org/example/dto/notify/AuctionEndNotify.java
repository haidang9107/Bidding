package org.example.dto.notify;

import org.example.dto.response.ProductResponse;

public class AuctionEndNotify {
    private int auctionId;
    private String winnerAccountname;
    private long finalPrice;
    private String productName;
    private ProductResponse productDetail;

    public AuctionEndNotify() {}

    public AuctionEndNotify(int auctionId, String winnerAccountname, long finalPrice, String productName, ProductResponse productDetail) {
        this.auctionId = auctionId;
        this.winnerAccountname = winnerAccountname;
        this.finalPrice = finalPrice;
        this.productName = productName;
        this.productDetail = productDetail;
    }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public String getWinnerAccountname() { return winnerAccountname; }
    public void setWinnerAccountname(String winnerAccountname) { this.winnerAccountname = winnerAccountname; }

    public long getFinalPrice() { return finalPrice; }
    public void setFinalPrice(long finalPrice) { this.finalPrice = finalPrice; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public ProductResponse getProductDetail() { return productDetail; }
    public void setProductDetail(ProductResponse productDetail) { this.productDetail = productDetail; }
}
