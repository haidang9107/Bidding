package org.example.server.model;

import java.sql.Timestamp;

public class Admin extends User {

    // =========================
    // Constructor rỗng
    // =========================
    public Admin() {
        super();
    }

    // =========================
    // Constructor đầy đủ
    // =========================
    public Admin(String userId,
                 String username,
                 String password,
                 String email,
                 String phoneNumber,
                 String gender,
                 String avt,
                 double balance,
                 Timestamp createdAt) {

        super(
                userId,
                username,
                password,
                email,
                phoneNumber,
                gender,
                avt,
                balance,
                createdAt
        );
    }

    // =========================
    // Xem toàn bộ user
    // =========================
    public void viewAllUsers() {

        System.out.println(
                "Admin is viewing all users"
        );
    }

    // =========================
    // Xóa user
    // =========================
    public void deleteUser(String targetUserId) {

        System.out.println(
                "Admin deleted user: " + targetUserId
        );
    }

    // =========================
    // Khóa tài khoản user
    // =========================
    public void banUser(String targetUserId) {

        System.out.println(
                "Admin banned user: " + targetUserId
        );
    }

    // =========================
    // Mở khóa tài khoản user
    // =========================
    public void unbanUser(String targetUserId) {

        System.out.println(
                "Admin unbanned user: " + targetUserId
        );
    }

    // =========================
    // Quản lý hệ thống
    // =========================
    public void manageSystem() {

        System.out.println(
                "Admin is managing the system"
        );
    }

    // =========================
    // Reset số dư user
    // =========================
    public void resetBalance(User user) {

        user.setBalance(0);

        System.out.println(
                "Reset balance of user: "
                        + user.getUsername()
        );
    }

    // =========================
    // Hiển thị thông tin admin
    // =========================
    @Override
    public String toString() {

        return "Admin{" +
                "userId='" + getUserId() + '\'' +
                ", username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", balance=" + getBalance() +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}