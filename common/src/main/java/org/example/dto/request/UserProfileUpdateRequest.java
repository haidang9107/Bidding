package org.example.dto.request;

/**
 * DTO for updating user profile information.
 */
public class UserProfileUpdateRequest {
    private String email;
    private String avt;
    private String fullname;

    /**
     * Default constructor for UserProfileUpdateRequest.
     */
    public UserProfileUpdateRequest() {}

    /**
     * Gets the fullname.
     * @return the fullname
     */
    public String getFullname() { return fullname; }

    /**
     * Sets the fullname.
     * @param fullname the fullname to set
     */
    public void setFullname(String fullname) { this.fullname = fullname; }

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
