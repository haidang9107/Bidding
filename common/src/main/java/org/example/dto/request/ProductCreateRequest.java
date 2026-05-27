package org.example.dto.request;

import org.example.model.enums.ItemCategory;

/**
 * DTO for creating a new product in the seller's inventory.
 *
 * <p>Unlike {@link ProductAddRequest}, this does NOT carry any auction
 * parameters (starting price, step price, duration). The product is created
 * in the "stock" state — owned by the seller, not on the marketplace. The
 * seller can later open an auction for it via {@code AUCTION_OPEN}.
 */
public class ProductCreateRequest {
    private String name;
    private String description;
    private ItemCategory category;
    private Object metadata;        // Category-specific fields (brand, warrantyMonths, ...)
    private String imageUrl;        // Direct image URL (e.g. Cloudinary)

    public ProductCreateRequest() {}

    public ProductCreateRequest(String name, String description, ItemCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public Object getMetadata() { return metadata; }
    public void setMetadata(Object metadata) { this.metadata = metadata; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
