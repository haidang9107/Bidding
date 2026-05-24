package org.example.dto.response;

import java.util.List;

/**
 * Generic DTO for returning paginated data with metadata.
 */
public class PagedResponse<T> {
    private List<T> items;
    private long totalItems;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    /**
     * Default constructor for PagedResponse.
     */
    public PagedResponse() {}

    /**
     * Constructs a PagedResponse with items and pagination metadata.
     * @param items the list of items for the current page
     * @param totalItems the total number of items across all pages
     * @param currentPage the current page index
     * @param pageSize the number of items per page
     */
    public PagedResponse(List<T> items, long totalItems, int currentPage, int pageSize) {
        this.items = items;
        this.totalItems = totalItems;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
    }

    /**
     * Gets the list of items.
     * @return the list of items
     */
    public List<T> getItems() { return items; }

    /**
     * Sets the list of items.
     * @param items the list of items to set
     */
    public void setItems(List<T> items) { this.items = items; }

    /**
     * Gets the total number of items.
     * @return the total items
     */
    public long getTotalItems() { return totalItems; }

    /**
     * Sets the total number of items.
     * @param totalItems the total items to set
     */
    public void setTotalItems(long totalItems) { this.totalItems = totalItems; }

    /**
     * Gets the total number of pages.
     * @return the total pages
     */
    public int getTotalPages() { return totalPages; }

    /**
     * Sets the total number of pages.
     * @param totalPages the total pages to set
     */
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    /**
     * Gets the current page number.
     * @return the current page
     */
    public int getCurrentPage() { return currentPage; }

    /**
     * Sets the current page number.
     * @param currentPage the current page to set
     */
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    /**
     * Gets the page size.
     * @return the page size
     */
    public int getPageSize() { return pageSize; }

    /**
     * Sets the page size.
     * @param pageSize the page size to set
     */
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
