package org.example.dto.request;

import org.example.model.enums.ItemCategory;

/**
 * DTO for adding a new product/auction.
 */
public class ProductAddRequest {
    private String name;
    private String description;
    private long startingPrice;
    private ItemCategory category;
    private Object metadata; // Specific fields for Electronics, Art, etc.

    /**
     * Default constructor for ProductAddRequest.
     */
    public ProductAddRequest() {}

    /**
     * Constructs a ProductAddRequest with basic details.
     * @param name the product name
     * @param description the product description
     * @param startingPrice the starting price for the auction
     * @param category the item category
     */
    public ProductAddRequest(String name, String description, long startingPrice, ItemCategory category) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.category = category;
    }

    /**
     * Gets the product name.
     * @return the name
     */
    public String getName() { return name; }

    /**
     * Sets the product name.
     * @param name the name to set
     */
    public void setName(String name) { this.name = name; }

    /**
     * Gets the product description.
     * @return the description
     */
    public String getDescription() { return description; }

    /**
     * Sets the product description.
     * @param description the description to set
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Gets the starting price.
     * @return the starting price
     */
    public long getStartingPrice() { return startingPrice; }

    /**
     * Sets the starting price.
     * @param startingPrice the starting price to set
     */
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    /**
     * Gets the item category.
     * @return the category
     */
    public ItemCategory getCategory() { return category; }

    /**
     * Sets the item category.
     * @param category the category to set
     */
    public void setCategory(ItemCategory category) { this.category = category; }

    /**
     * Gets the category-specific metadata.
     * @return the metadata object
     */
    public Object getMetadata() { return metadata; }

    /**
     * Sets the category-specific metadata.
     * @param metadata the metadata to set
     */
    public void setMetadata(Object metadata) { this.metadata = metadata; }
}
