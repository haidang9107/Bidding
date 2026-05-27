package org.example.model.product;

import org.example.model.enums.ItemCategory;

/**
 * Represents a product that doesn't fit into specialized categories.
 */
public class OtherItem extends Product {

    /**
     * Default constructor.
     */
    public OtherItem() {
        super();
        setCategory(ItemCategory.OTHER);
    }

    /**
     * Constructs an OtherItem product with all fields.
     * @param productId        The unique product ID.
     * @param name             The product name.
     * @param description      The product description.
     * @param imageUrl         The image URL.
     * @param ownerAccountname The current owner of the product.
     */
    public OtherItem(int productId, String name, String description, String imageUrl,
                     String ownerAccountname) {
        super(productId, name, description, imageUrl, ItemCategory.OTHER, ownerAccountname);
    }
}
