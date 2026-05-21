package org.example.dto;

/**
 * DTO for admin to control user status (ban/unban).
 */
public class AdminUserControlRequest {
    private String targetAccountname;
    private int status; // 0 for Active, 1 for Banned

    public AdminUserControlRequest() {}

    public AdminUserControlRequest(String targetAccountname, int status) {
        this.targetAccountname = targetAccountname;
        this.status = status;
    }

    public String getTargetAccountname() { return targetAccountname; }
    public void setTargetAccountname(String targetAccountname) { this.targetAccountname = targetAccountname; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
