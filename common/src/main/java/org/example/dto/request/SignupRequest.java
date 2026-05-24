package org.example.dto.request;

/**
 * DTO for user registration requests.
 * Role is omitted as it is assigned by the server (default: MEMBER).
 */
public class SignupRequest {
    private String username;
    private String password;
    private String email;

    /**
     * Default constructor for SignupRequest.
     */
    public SignupRequest() {}

    /**
     * Constructs a SignupRequest with specified username, password, and email.
     * @param username the desired username
     * @param password the desired password
     * @param email the user's email address
     */
    public SignupRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
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
}
