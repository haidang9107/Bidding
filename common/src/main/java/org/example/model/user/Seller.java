package org.example.model.user;

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
}