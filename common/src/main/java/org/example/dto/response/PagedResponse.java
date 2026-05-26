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
     */
    public PagedResponse(List<T> items, long totalItems, int currentPage, int pageSize) {
        this.items = items;
        this.totalItems = totalItems;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
    }

    // ==========================================
    // CÁC HÀM ALIAS GIÚP FIX LỖI CODETEST CŨ
    // ==========================================

    /**
     * Alias cho hàm getItems() để tương thích với code test cũ dùng getContent()
     */
    public List<T> getContent() {
        return this.items;
    }

    /**
     * Alias cho hàm getTotalItems() để tương thích với code test cũ dùng getTotal()
     */
    public long getTotal() {
        return this.totalItems;
    }

    // ==========================================
    // CÁC GETTER VÀ SETTER GỐC
    // ==========================================

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}