package org.example.server.exception;

/**
 * Thrown when an error occurs in the auction business logic.
 */
public class AuctionException extends BaseAppException {
    /**
     * Constructor for AuctionException with a message.
     *
     * @param message the detail message
     */
    public AuctionException(String message) {
        super(message, "AUCTION_ERROR");
    }

    /**
     * Constructor for AuctionException with a message and error code.
     *
     * @param message the detail message
     * @param errorCode the application-specific error code
     */
    public AuctionException(String message, String errorCode) {
        super(message, errorCode);
    }
}
