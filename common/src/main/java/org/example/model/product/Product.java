package org.example.model.product;

import org.example.model.enums.ItemCategory;
import java.sql.Timestamp;

/**
 * Represents a physical product that a user owns.
 * A product is independent from the concept of an auction: the same product
 * can be put up for auction multiple times (1-to-many with Auction).
 */
public abstract class Product {

    private int productId;
    private String name;
    private String description;
    private String imageUrl;
    private ItemCategory category;
    private String ownerAccountname;
    private boolean inAuction;
    private Timestamp withdrawnAt;

    /**
     * Default constructor.
     */
    public Product() {
    }

    /**
     * Constructs a Product with the core identifying fields.
     * @param productId       The unique product ID.
     * @param name            The product name.
     * @param description     The product description.
     * @param imageUrl        The image URL.
     * @param category        The product category.
     * @param ownerAccountname The current owner of the product.
     */
    public Product(int productId, String name, String description, String imageUrl,
                   ItemCategory category, String ownerAccountname) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
        this.ownerAccountname = ownerAccountname;
    }

    // Getters and Setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public String getOwnerAccountname() { return ownerAccountname; }
    public void setOwnerAccountname(String ownerAccountname) { this.ownerAccountname = ownerAccountname; }

    public boolean isInAuction() { return inAuction; }
    public void setInAuction(boolean inAuction) { this.inAuction = inAuction; }

    public Timestamp getWithdrawnAt() { return withdrawnAt; }
    public void setWithdrawnAt(Timestamp withdrawnAt) { this.withdrawnAt = withdrawnAt; }
}
