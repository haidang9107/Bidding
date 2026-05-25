package org.example.dto.request;

/**
 * DTO for admin to control user status (ban/unban).
 */
public class AdminUserControlRequest {
    private String targetAccountname;
    private int status; // 0 for Active, 1 for Banned

    /**
     * Default constructor for AdminUserControlRequest.
     */
    public AdminUserControlRequest() {}

    /**
     * Constructs an AdminUserControlRequest with target account and desired status.
     * @param targetAccountname the account name of the target user
     * @param status the status to set (0 for Active, 1 for Banned)
     */
    public AdminUserControlRequest(String targetAccountname, int status) {
        this.targetAccountname = targetAccountname;
        this.status = status;
    }

    /**
     * Gets the target account name.
     * @return the target account name
     */
    public String getTargetAccountname() { return targetAccountname; }

    /**
     * Sets the target account name.
     * @param targetAccountname the target account name to set
     */
    public void setTargetAccountname(String targetAccountname) { this.targetAccountname = targetAccountname; }

    /**
     * Gets the user status.
     * @return the status (0 for Active, 1 for Banned)
     */
    public int getStatus() { return status; }

    /**
     * Sets the user status.
     * @param status the status to set
     */
    public void setStatus(int status) { this.status = status; }
}
