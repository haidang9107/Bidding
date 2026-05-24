package org.example.server.exception;

/**
 * Thrown when an error occurs in financial operations.
 */
public class FinanceException extends BaseAppException {
    /**
     * Constructs a FinanceException with a message.
     * @param message The error message.
     */
    public FinanceException(String message) {
        super(message, "FINANCE_ERROR");
    }

    /**
     * Constructs a FinanceException with a message and error code.
     * @param message The error message.
     * @param errorCode The application-specific error code.
     */
    public FinanceException(String message, String errorCode) {
        super(message, errorCode);
    }
}
