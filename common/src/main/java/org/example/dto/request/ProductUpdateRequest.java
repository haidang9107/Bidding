package org.example.dto.request;

import org.example.model.enums.ItemCategory;

/**
 * DTO for updating an existing product in the seller's inventory.
 * Similar to {@link ProductCreateRequest} but includes a {@code productId}.
 */
public class ProductUpdateRequest {
    private int productId;
    private String name;
    private String description;
    private ItemCategory category;
    private Object metadata;        // Category-specific fields (brand, warrantyMonths, ...)
    private String imageUrl;        // Direct image URL (e.g. Cloudinary)

    public ProductUpdateRequest() {}

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

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
