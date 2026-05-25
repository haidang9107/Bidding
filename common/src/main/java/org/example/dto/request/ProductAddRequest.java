package org.example.dto.request;

import org.example.model.enums.ItemCategory;

import java.sql.Timestamp;

/**
 * DTO for creating a new product together with its first auction.
 *
 * <p>Logically split into two parts:
 * <ul>
 *   <li><b>Product data</b>: {@code name}, {@code description}, {@code category}, {@code metadata}</li>
 *   <li><b>Auction parameters</b>: {@code startingPrice}, {@code stepPrice}, {@code buyNowPrice},
 *       {@code startTime}, {@code endTime}</li>
 * </ul>
 * The server creates a {@code Product} row first and then opens a new {@code Auction}
 * referencing it.
 */
public class ProductAddRequest {
    // Product fields
    private String name;
    private String description;
    private ItemCategory category;
    private Object metadata; // Category-specific fields (brand, warrantyMonths, artist, etc.)
    private String imageBase64; // Optional: image content encoded as Base64 (data-URL or raw)
    private String imageMimeType; // Optional: MIME type (e.g. "image/png", "image/jpeg")

    // Auction fields
    private long startingPrice;
    private Long stepPrice;     // Optional: defaults to startingPrice / 10
    private Long buyNowPrice;   // Optional
    private Timestamp startTime; // Optional: defaults to now
    private Timestamp endTime;   // Optional: defaults to now + 7 days

    /**
     * Default constructor for ProductAddRequest.
     */
    public ProductAddRequest() {}

    /**
     * Constructs a ProductAddRequest with the minimal fields.
     * @param name          the product name
     * @param description   the product description
     * @param startingPrice the starting price for the auction
     * @param category      the product category
     */
    public ProductAddRequest(String name, String description, long startingPrice, ItemCategory category) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.category = category;
    }

    // Product getters/setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public Object getMetadata() { return metadata; }
    public void setMetadata(Object metadata) { this.metadata = metadata; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getImageMimeType() { return imageMimeType; }
    public void setImageMimeType(String imageMimeType) { this.imageMimeType = imageMimeType; }

    // Auction getters/setters
    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    public Long getStepPrice() { return stepPrice; }
    public void setStepPrice(Long stepPrice) { this.stepPrice = stepPrice; }

    public Long getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(Long buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
}
