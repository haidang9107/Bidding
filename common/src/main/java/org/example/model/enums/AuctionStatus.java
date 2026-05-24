package org.example.model.enums;

/**
 * Represents the status of an auction session.
 */
public enum AuctionStatus {
    OPEN(0),      // Mới mở, đang chờ
    RUNNING(1),   // Đang diễn ra
    FINISHED(2),  // Đã kết thúc
    PAID(3),      // Đã thanh toán
    CANCELED(4);  // Đã hủy

    private final int value;

    AuctionStatus(int value) {
        this.value = value;
    }

    /**
     * Gets the integer value of the status.
     * @return The integer value.
     */
    public int getValue() {
        return value;
    }

    /**
     * Maps an integer to an AuctionStatus.
     * @param value The integer value.
     * @return The corresponding AuctionStatus, or OPEN if not found.
     */
    public static AuctionStatus fromInt(int value) {
        for (AuctionStatus status : AuctionStatus.values()) {
            if (status.value == value) {
                return status;
            }
        }
        return OPEN; // Default
    }
}
