package org.example.server.model;

import java.sql.Timestamp;
/**
 * Lớp cơ sở (Base Class) cho tất cả các thực thể trong hệ thống.
 * Chứa các thuộc tính dùng chung để đảm bảo tính nhất quán giữa Java và Database.
 */
public abstract class Entity {

    // Tương ứng với trường created_at TIMESTAMP trong database
    protected Timestamp createdAt;

    // Constructor mặc định
    public Entity() {
    }

    // Constructor có tham số để khởi tạo từ dữ liệu database
    public Entity(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // Getter và Setter
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Ghi đè phương thức toString để hỗ trợ việc debug/in dữ liệu dễ dàng hơn
     */
    @Override
    public String toString() {
        return "Entity{" +
                "createdAt=" + createdAt +
                '}';
    }
}