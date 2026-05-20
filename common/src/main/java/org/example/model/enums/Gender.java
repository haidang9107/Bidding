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

    public int getValue() {
        return value;
    }

    public static Gender fromInt(int value) {
        for (Gender gender : Gender.values()) {
            if (gender.value == value) {
                return gender;
            }
        }
        return MALE; // Default
    }
}
