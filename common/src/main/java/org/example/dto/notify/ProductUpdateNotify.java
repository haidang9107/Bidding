package org.example.dto.notify;

import org.example.dto.response.ProductResponse;

/**
 * Notification DTO for product updates.
 */
public class ProductUpdateNotify {
    private ProductResponse product;

    /**
     * Default constructor for ProductUpdateNotify.
     */
    public ProductUpdateNotify() {}

    /**
     * Constructs a ProductUpdateNotify with the specified product.
     * @param product the product response containing updated information
     */
    public ProductUpdateNotify(ProductResponse product) {
        this.product = product;
    }

    /**
     * Gets the product response.
     * @return the product response
     */
    public ProductResponse getProduct() { return product; }

    /**
     * Sets the product response.
     * @param product the product response to set
     */
    public void setProduct(ProductResponse product) { this.product = product; }
}
