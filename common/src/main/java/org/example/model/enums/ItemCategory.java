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

    public int getValue() {
        return value;
    }

    public static ItemCategory fromInt(int value) {
        for (ItemCategory category : ItemCategory.values()) {
            if (category.value == value) {
                return category;
            }
        }
        return OTHER; // Default
    }
}
