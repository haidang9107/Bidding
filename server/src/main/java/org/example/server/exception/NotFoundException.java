package org.example.server.exception;

/**
 * Thrown when a requested resource is not found.
 */
public class NotFoundException extends BaseAppException {
    public NotFoundException(String message) {
        super(message, "NOT_FOUND");
    }

    public NotFoundException(String message, String errorCode) {
        super(message, errorCode);
    }
}
