package org.example.model.user;

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
}