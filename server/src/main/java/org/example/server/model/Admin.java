package org.example.server.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Thực thể đại diện cho Quản trị viên (Admin).
 * Kế thừa từ User, do đó kế thừa luôn cả Entity.
 */
public class Admin extends User {

    // Bạn có thể thêm các thuộc tính riêng cho Admin ở đây nếu cần
    // Ví dụ: cấp độ quản trị, mã xác thực bảo mật riêng, v.v.
    private int adminLevel;

    public Admin() {
        super();
        this.setRole("ADMIN"); // Mặc định role là ADMIN khi khởi tạo
    }

    // Constructor đầy đủ để lấy dữ liệu từ database
    public Admin(String userId, String username, String password, String email,
                 String phoneNumber, String gender, String avt,
                 BigDecimal balance, Timestamp createdAt, int adminLevel) {

        // Gọi lại constructor của lớp cha (User)
        super(userId, username, password, email, phoneNumber, gender, avt, balance, "ADMIN", createdAt);
        this.adminLevel = adminLevel;
    }

    // Getter và Setter riêng cho Admin
    public int getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(int adminLevel) {
        this.adminLevel = adminLevel;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "userId='" + getUserId() + '\'' +
                ", username='" + getUsername() + '\'' +
                ", adminLevel=" + adminLevel +
                ", role='" + getRole() + '\'' +
                '}';
    }
}