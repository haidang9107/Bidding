package org.example.server.exception;

/**
 * Thrown when an error occurs during database operations.
 */
public class DatabaseException extends BaseAppException {
    /**
     * Constructor for DatabaseException with a message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, "DB_ERROR", cause);
    }

    /**
     * Constructor for DatabaseException with a message.
     *
     * @param message the detail message
     */
    public DatabaseException(String message) {
        super(message, "DB_ERROR");
    }
}
