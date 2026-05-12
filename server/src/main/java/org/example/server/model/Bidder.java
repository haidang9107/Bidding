package org.example.server.model;

import java.sql.Timestamp;

public class Bidder extends User {

    // =========================
    // Constructor rỗng
    // =========================
    public Bidder() {
        super();
    }

    // =========================
    // Constructor đầy đủ
    // =========================
    public Bidder(String userId,
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
    // Đặt giá đấu
    // =========================
    public void placeBid(double amount) {

        System.out.println(
                getUsername() +
                        " placed a bid: $" +
                        amount
        );
    }

    // =========================
    // Nạp tiền vào tài khoản
    // =========================
    public void deposit(double amount) {

        if (amount <= 0) {

            System.out.println(
                    "Deposit amount must be greater than 0"
            );

            return;
        }

        setBalance(
                getBalance() + amount
        );

        System.out.println(
                getUsername() +
                        " deposited $" +
                        amount
        );
    }

    // =========================
    // Trừ tiền khỏi tài khoản
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
    // Kiểm tra số dư
    // =========================
    public void checkBalance() {

        System.out.println(
                "Current balance: $" +
                        getBalance()
        );
    }

    // =========================
    // Hiển thị thông tin bidder
    // =========================
    @Override
    public String toString() {

        return "Bidder{" +
                "userId='" + getUserId() + '\'' +
                ", username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", balance=" + getBalance() +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}