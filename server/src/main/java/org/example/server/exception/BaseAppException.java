package org.example.server.exception;

/**
 * Base exception for the bidding application.
 */
public abstract class BaseAppException extends RuntimeException {
    private final String errorCode;

    /**
     * Constructor for BaseAppException with a message and error code.
     *
     * @param message the detail message
     * @param errorCode the application-specific error code
     */
    public BaseAppException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructor for BaseAppException with a message, error code, and cause.
     *
     * @param message the detail message
     * @param errorCode the application-specific error code
     * @param cause the cause of the exception
     */
    public BaseAppException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Gets the application-specific error code.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}
