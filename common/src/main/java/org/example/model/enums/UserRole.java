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

    public int getValue() {
        return value;
    }

    public static UserRole fromInt(int value) {
        for (UserRole role : UserRole.values()) {
            if (role.value == value) {
                return role;
            }
        }
        return MEMBER; // Default
    }
}
