package org.example.server.repository;

import org.example.server.model.Admin;
import org.example.server.model.Bidder;
import org.example.server.model.Seller;
import org.example.server.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    // =========================
    // Connection tới database
    // =========================
    private Connection connection;

    // =========================
    // Constructor
    // =========================
    public UserDao(Connection connection) {
        this.connection = connection;
    }

    // =========================
    // Lấy toàn bộ user
    // =========================
    public List<User> getAllUsers() throws SQLException {

        List<User> users = new ArrayList<>();

        String sql = "SELECT * FROM users";

        try (
                Statement stmt = connection.createStatement();

                ResultSet rs = stmt.executeQuery(sql)
        ) {

            while (rs.next()) {

                // =========================
                // Lấy role từ database
                // =========================
                String role = rs.getString("role");

                User user;

                // =========================
                // Mapping theo role
                // =========================
                if (role.equalsIgnoreCase("ADMIN")) {

                    user = new Admin(
                            rs.getString("user_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email"),
                            rs.getString("phonenumber"),
                            rs.getString("gender"),
                            rs.getString("avt"),
                            rs.getDouble("balance"),
                            rs.getTimestamp("created_at")
                    );

                } else if (role.equalsIgnoreCase("SELLER")) {

                    user = new Seller(
                            rs.getString("user_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email"),
                            rs.getString("phonenumber"),
                            rs.getString("gender"),
                            rs.getString("avt"),
                            rs.getDouble("balance"),
                            rs.getTimestamp("created_at")
                    );

                } else {

                    user = new Bidder(
                            rs.getString("user_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email"),
                            rs.getString("phonenumber"),
                            rs.getString("gender"),
                            rs.getString("avt"),
                            rs.getDouble("balance"),
                            rs.getTimestamp("created_at")
                    );
                }

                // =========================
                // Add vào list
                // =========================
                users.add(user);
            }
        }

        return users;
    }
}