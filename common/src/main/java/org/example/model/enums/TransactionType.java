package org.example.model.enums;

/**
 * Represents the type of a financial transaction.
 */
public enum TransactionType {
    DEPOSIT(1),
    WITHDRAW(2),
    TRANSFER(3),
    AUCTION_PAYMENT(4),
    REFUND(5);

    private final int value;

    TransactionType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Gets the TransactionType from an integer value.
     * @param value The integer value.
     * @return The corresponding TransactionType.
     */
    public static TransactionType fromInt(int value) {
        for (TransactionType type : TransactionType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return DEPOSIT; // Default
    }
}
