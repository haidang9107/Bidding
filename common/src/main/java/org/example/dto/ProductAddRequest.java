package org.example.dto;

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

    public ProductAddRequest() {}

    public ProductAddRequest(String name, String description, long startingPrice, ItemCategory category) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.category = category;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public Object getMetadata() { return metadata; }
    public void setMetadata(Object metadata) { this.metadata = metadata; }
}
