package org.example.model.product;

import org.example.model.enums.AuctionStatus;
import org.example.model.enums.ItemCategory;
import java.sql.Timestamp;

/**
 * Represents a generic item that doesn't fit into specialized categories.
 */
public class OtherItem extends Item {

    public OtherItem() {
        super();
        setCategory(ItemCategory.OTHER);
    }

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
