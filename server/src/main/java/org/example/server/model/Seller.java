package org.example.server.model;

import java.sql.Timestamp;

public class Seller extends User {

    // =========================
    // Constructor rỗng
    // =========================
    public Seller() {
        super();
    }

    // =========================
    // Constructor đầy đủ
    // =========================
    public Seller(String userId,
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
    // Tạo sản phẩm đấu giá
    // =========================
    public void createProduct(String productName) {

        System.out.println(
                getUsername() +
                        " created product: " +
                        productName
        );
    }

    // =========================
    // Xóa sản phẩm
    // =========================
    public void removeProduct(String productName) {

        System.out.println(
                getUsername() +
                        " removed product: " +
                        productName
        );
    }

    // =========================
    // Cập nhật sản phẩm
    // =========================
    public void updateProduct(String productName) {

        System.out.println(
                getUsername() +
                        " updated product: " +
                        productName
        );
    }

    // =========================
    // Xem danh sách sản phẩm
    // =========================
    public void viewProducts() {

        System.out.println(
                getUsername() +
                        " is viewing all owned products"
        );
    }

    // =========================
    // Kiểm tra số dư
    // =========================
    public void checkBalance() {

        System.out.println(
                "Current balance: $" +
                        getBalance()
        );
    }

    // =========================
    // Rút tiền
    // =========================
    public void withdraw(double amount) {

        if (amount <= 0) {

            System.out.println(
                    "Withdraw amount must be greater than 0"
            );

            return;
        }

        if (amount > getBalance()) {

            System.out.println(
                    "Insufficient balance"
            );

            return;
        }

        setBalance(
                getBalance() - amount
        );

        System.out.println(
                getUsername() +
                        " withdrew $" +
                        amount
        );
    }

    // =========================
    // Hiển thị thông tin seller
    // =========================
    @Override
    public String toString() {

        return "Seller{" +
                "userId='" + getUserId() + '\'' +
                ", username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", balance=" + getBalance() +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}