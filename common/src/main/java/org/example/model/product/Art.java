package org.example.model.product;

import org.example.model.enums.ItemCategory;
import org.example.model.enums.AuctionStatus;
import java.sql.Timestamp;

/**
 * Represents an artwork item.
 */
public class Art extends Item {

    private String artist;
    private String artType;

    public Art() {
        super();
        this.setCategory(ItemCategory.ART);
    }

    public Art(int productId, String productName, String description, long startingPrice, 
               long currentPrice, long stepPrice, int sellerId, Integer winnerId, 
               AuctionStatus status, Timestamp startTime, Timestamp endTime, 
               int version, Timestamp createdAt, String artist, String artType) {
        super(productId, productName, description, startingPrice, currentPrice, stepPrice, 
              sellerId, winnerId, ItemCategory.ART, status, startTime, endTime, version, createdAt);
        this.artist = artist;
        this.artType = artType;
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getArtType() { return artType; }
    public void setArtType(String artType) { this.artType = artType; }
}
