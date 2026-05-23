package org.example.server.exception;

/**
 * Thrown when an error occurs in financial operations.
 */
public class FinanceException extends BaseAppException {
    public FinanceException(String message) {
        super(message, "FINANCE_ERROR");
    }

    public FinanceException(String message, String errorCode) {
        super(message, errorCode);
    }
}
