package org.example.server.model;

import java.sql.Timestamp;

public abstract class Entity {
    protected String id;        // Khớp với user_id hoặc id trong database
    protected String name;      // Tên hiển thị chung (username, product_name, ...)
    protected Timestamp createdAt; // Khớp với cột created_at

    // Constructor mặc định
    public Entity() {
    }

    // Constructor với ID và Name (Dùng cho User.java trước đó của bạn)
    public Entity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Constructor đầy đủ
    public Entity(String id, String name, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    // Getter và Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}