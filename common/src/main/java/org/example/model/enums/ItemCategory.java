package org.example.model.enums;

/**
 * Represents the category of an item.
 */
public enum ItemCategory {
    OTHER(0),
    ELECTRONICS(1),
    ART(2),
    VEHICLE(3);

    private final int value;

    ItemCategory(int value) {
        this.value = value;
    }

    /**
     * Gets the integer value of the category.
     * @return The integer value.
     */
    public int getValue() {
        return value;
    }

    /**
     * Maps an integer to an ItemCategory.
     * @param value The integer value.
     * @return The corresponding ItemCategory, or OTHER if not found.
     */
    public static ItemCategory fromInt(int value) {
        for (ItemCategory category : ItemCategory.values()) {
            if (category.value == value) {
                return category;
            }
        }
        return OTHER; // Default
    }
}
