package org.example.model.enums;

/**
 * Represents the gender of a user.
 */
public enum Gender {
    MALE(0),
    FEMALE(1),
    OTHER(2);

    private final int value;

    Gender(int value) {
        this.value = value;
    }

    /**
     * Gets the integer value of the gender.
     * @return The integer value.
     */
    public int getValue() {
        return value;
    }

    /**
     * Maps an integer to a Gender.
     * @param value The integer value.
     * @return The corresponding Gender, or MALE if not found.
     */
    public static Gender fromInt(int value) {
        for (Gender gender : Gender.values()) {
            if (gender.value == value) {
                return gender;
            }
        }
        return MALE; // Default
    }
}
