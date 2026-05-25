package org.example.dto.request;

/**
 * DTO for updating user profile information.
 */
public class UserProfileUpdateRequest {
    private String email;
    private String avt;

    /**
     * Default constructor for UserProfileUpdateRequest.
     */
    public UserProfileUpdateRequest() {}

    /**
     * Constructs a UserProfileUpdateRequest with specified email and avatar URL.
     * @param email the user's email
     * @param avt the user's avatar URL or identifier
     */
    public UserProfileUpdateRequest(String email, String avt) {
        this.email = email;
        this.avt = avt;
    }

    /**
     * Gets the email.
     * @return the email
     */
    public String getEmail() { return email; }

    /**
     * Sets the email.
     * @param email the email to set
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Gets the avatar.
     * @return the avatar
     */
    public String getAvt() { return avt; }

    /**
     * Sets the avatar.
     * @param avt the avatar to set
     */
    public void setAvt(String avt) { this.avt = avt; }
}
