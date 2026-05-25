package org.example.dto.request;

/**
 * DTO for requesting a specific page of data.
 */
public class PaginationRequest {
    private int page = 1; // 1-based index
    private int pageSize = 10;

    /**
     * Default constructor for PaginationRequest.
     */
    public PaginationRequest() {}

    /**
     * Constructs a PaginationRequest with specified page and page size.
     * @param page the page number (1-based)
     * @param pageSize the number of items per page
     */
    public PaginationRequest(int page, int pageSize) {
        this.page = Math.max(1, page);
        this.pageSize = Math.max(1, pageSize);
    }

    /**
     * Gets the page number.
     * @return the page number
     */
    public int getPage() { return page; }

    /**
     * Sets the page number.
     * @param page the page number to set
     */
    public void setPage(int page) { this.page = page; }

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

    /**
     * Calculates the offset for database queries.
     * @return the offset
     */
    public int getOffset() {
        return (page - 1) * pageSize;
    }
}
