package org.example.dto.request;

import org.example.model.enums.ItemCategory;

/**
 * DTO for advanced product searching and filtering.
 * Extends {@link PaginationRequest} to support paged search results.
 */
public class ProductSearchRequest extends PaginationRequest {
    /** Keyword to search in product name or description. */
    private String keyword;
    /** Category to filter products by. */
    private ItemCategory category;
    /** Minimum current price (or 0 for inventory items). */
    private Long minPrice;
    /** Maximum current price (or 0 for inventory items). */
    private Long maxPrice;
    /** 
     * Filter by auction status: 
     * true: only products in active auctions, 
     * false: only products in inventory (not in auction),
     * null: all products.
     */
    private Boolean isInAuction;
    /** Sort criteria: NEWEST, PRICE_ASC, or PRICE_DESC. */
    private String sortBy;

    /**
     * Default constructor for GSON.
     */
    public ProductSearchRequest() {
        super();
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public Long getMinPrice() { return minPrice; }
    public void setMinPrice(Long minPrice) { this.minPrice = minPrice; }

    public Long getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Long maxPrice) { this.maxPrice = maxPrice; }

    public Boolean getInAuction() { return isInAuction; }
    public void setInAuction(Boolean isInAuction) { this.isInAuction = isInAuction; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
}
