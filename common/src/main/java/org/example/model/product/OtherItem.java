package org.example.model.product;

import org.example.model.enums.AuctionStatus;
import org.example.model.enums.ItemCategory;
import java.sql.Timestamp;

/**
 * Represents a generic item that doesn't fit into specialized categories.
 */
public class OtherItem extends Item {

    /**
     * Default constructor for OtherItem.
     */
    public OtherItem() {
        super();
        setCategory(ItemCategory.OTHER);
    }

    /**
     * Constructs an OtherItem with all fields.
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
     */
    public OtherItem(int productId, String name, String description, String imageUrl, 
                     long startingPrice, long currentPrice, long stepPrice, 
                     String sellerAccountname, String winnerAccountname, 
                     AuctionStatus status, Timestamp startTime, Timestamp endTime, 
                     int version) {
        super(productId, name, description, imageUrl, startingPrice, currentPrice, 
              stepPrice, sellerAccountname, winnerAccountname, ItemCategory.OTHER, 
              status, startTime, endTime, version);
    }
}
