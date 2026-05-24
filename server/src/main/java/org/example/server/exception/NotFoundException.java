package org.example.server.exception;

/**
 * Thrown when a requested resource is not found.
 */
public class NotFoundException extends BaseAppException {
    /**
     * Constructs a NotFoundException with a message.
     * @param message The error message.
     */
    public NotFoundException(String message) {
        super(message, "NOT_FOUND");
    }

    /**
     * Constructs a NotFoundException with a message and error code.
     * @param message The error message.
     * @param errorCode The application-specific error code.
     */
    public NotFoundException(String message, String errorCode) {
        super(message, errorCode);
    }
}
