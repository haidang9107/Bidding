package org.example.model.user;

import org.example.model.product.Item;

/**
 * Interface defining the behaviors of a seller.
 */
public interface ISeller {
    void createAuction(Item item);
    void cancelAuction(int productId);
}
