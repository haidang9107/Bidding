package org.example.server.exception;

/**
 * Thrown when input data is invalid.
 */
public class ValidationException extends BaseAppException {
    /**
     * Constructor for ValidationException with a message.
     *
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }
}
