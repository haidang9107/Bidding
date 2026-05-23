package org.example.model.enums;

/**
 * Represents the status of an auction session.
 */
public enum AuctionStatus {
    PENDING(0),   // Sắp diễn ra
    ACTIVE(1),    // Đang diễn ra
    FINISHED(2),  // Đã kết thúc
    CANCELED(3),  // Đã hủy
    PAID(4);      // Đã thanh toán

    private final int value;

    AuctionStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AuctionStatus fromInt(int value) {
        for (AuctionStatus status : AuctionStatus.values()) {
            if (status.value == value) {
                return status;
            }
        }
        return PENDING; // Default
    }
}
