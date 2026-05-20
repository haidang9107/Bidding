package org.example.payload;

/**
 * Simple class to represent error details for the client.
 */
public class ErrorDetail {
    private String errorCode;
    private String description;
    private long timestamp;

    /**
     * Default constructor for ErrorDetail.
     * Initializes the timestamp to the current system time.
     */
    public ErrorDetail() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Constructor for ErrorDetail with code and description.
     *
     * @param errorCode the error code
     * @param description the description of the error
     */
    public ErrorDetail(String errorCode, String description) {
        this();
        this.errorCode = errorCode;
        this.description = description;
    }

    /**
     * Gets the error code.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the error code.
     *
     * @param errorCode the error code to set
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Gets the error description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the error description.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the timestamp when the error occurred.
     *
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the error occurred.
     *
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
