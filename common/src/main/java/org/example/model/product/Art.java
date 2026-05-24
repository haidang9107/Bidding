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

    /**
     * Default constructor for Art.
     */
    public Art() {
        super();
        this.setCategory(ItemCategory.ART);
    }

    /**
     * Constructs an Art item with all fields.
     * @param productId The unique product ID.
     * @param name The item name.
     * @param description The item description.
     * @param imageUrl The URL for the item's image.
     * @param startingPrice The initial price.
     * @param currentPrice The current highest bid.
     * @param stepPrice The minimum bid increment.
     * @param sellerAccountname The account name of the seller.
     * @param winnerAccountname The account name of the current winner.
     * @param status The auction status.
     * @param startTime The auction start time.
     * @param endTime The auction end time.
     * @param version The version for optimistic locking.
     * @param artist The artist's name.
     * @param artType The type of art.
     */
    public Art(int productId, String name, String description, String imageUrl, 
               long startingPrice, long currentPrice, long stepPrice, 
               String sellerAccountname, String winnerAccountname, 
               AuctionStatus status, Timestamp startTime, Timestamp endTime, 
               int version, String artist, String artType) {
        super(productId, name, description, imageUrl, startingPrice, currentPrice, stepPrice, 
              sellerAccountname, winnerAccountname, ItemCategory.ART, status, 
              startTime, endTime, version);
        this.artist = artist;
        this.artType = artType;
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getArtType() { return artType; }
    public void setArtType(String artType) { this.artType = artType; }
}
