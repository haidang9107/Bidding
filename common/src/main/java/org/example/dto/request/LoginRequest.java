package org.example.dto.request;

/**
 * Data Transfer Object for login requests.
 */
public class LoginRequest {
    private String username;
    private String password;

    /**
     * Default constructor for LoginRequest.
     */
    public LoginRequest() {}

    /**
     * Constructs a LoginRequest with specified username and password.
     * @param username the user's username
     * @param password the user's password
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Gets the username.
     * @return the username
     */
    public String getUsername() { return username; }

    /**
     * Sets the username.
     * @param username the username to set
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Gets the password.
     * @return the password
     */
    public String getPassword() { return password; }

    /**
     * Sets the password.
     * @param password the password to set
     */
    public void setPassword(String password) { this.password = password; }
}
