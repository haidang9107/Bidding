package org.example.model.enums;

/**
 * Represents the role of a user in the system.
 */
public enum UserRole {
    ADMIN(0),
    MEMBER(1);

    private final int value;

    UserRole(int value) {
        this.value = value;
    }

    /**
     * Gets the integer value of the role.
     * @return The integer value.
     */
    public int getValue() {
        return value;
    }

    /**
     * Maps an integer to a UserRole.
     * @param value The integer value.
     * @return The corresponding UserRole, or MEMBER if not found.
     */
    public static UserRole fromInt(int value) {
        for (UserRole role : UserRole.values()) {
            if (role.value == value) {
                return role;
            }
        }
        return MEMBER; // Default
    }
}
