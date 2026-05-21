package org.example.server.exception;

/**
 * Thrown when an authentication or authorization error occurs.
 */
public class AuthException extends BaseAppException {
    /**
     * Constructor for AuthException with a message.
     *
     * @param message the detail message
     */
    public AuthException(String message) {
        super(message, "AUTH_ERROR");
    }

    /**
     * Constructor for AuthException with a message and error code.
     *
     * @param message the detail message
     * @param errorCode the application-specific error code
     */
    public AuthException(String message, String errorCode) {
        super(message, errorCode);
    }
}
