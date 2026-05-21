package org.example.dto;

/**
 * DTO for updating user profile information.
 */
public class UserProfileUpdateRequest {
    private String email;
    private String avt;

    public UserProfileUpdateRequest() {}

    public UserProfileUpdateRequest(String email, String avt) {
        this.email = email;
        this.avt = avt;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvt() { return avt; }
    public void setAvt(String avt) { this.avt = avt; }
}
