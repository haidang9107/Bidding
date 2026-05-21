package org.example.dto;

/**
 * DTO for requesting a specific page of data.
 */
public class PaginationRequest {
    private int page = 1; // 1-based index
    private int pageSize = 10;

    public PaginationRequest() {}

    public PaginationRequest(int page, int pageSize) {
        this.page = Math.max(1, page);
        this.pageSize = Math.max(1, pageSize);
    }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getOffset() {
        return (page - 1) * pageSize;
    }
}
