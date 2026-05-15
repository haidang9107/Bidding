package org.example.server.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể đại diện cho Người bán (Seller).
 * Kế thừa từ User, do đó kế thừa luôn cả Entity.
 */
public class Seller extends User {

    // Thuộc tính riêng cho Seller (ví dụ: điểm uy tín từ 1-5 sao)
    private double rating;

    // Danh sách các mặt hàng mà người này đang bán
    private List<Item> itemsOffered;

    public Seller() {
        super();
        this.setRole("SELLER");
        this.itemsOffered = new ArrayList<>();
    }

    /**
     * Constructor đầy đủ để lấy dữ liệu từ database
     */
    public Seller(String userId, String username, String password, String email,
                  String phoneNumber, String gender, String avt,
                  BigDecimal balance, Timestamp createdAt, double rating) {

        // Gọi constructor của User và ép role về SELLER
        super(userId, username, password, email, phoneNumber, gender, avt, balance, "SELLER", createdAt);
        this.rating = rating;
        this.itemsOffered = new ArrayList<>();
    }

    // --- Getter và Setter ---

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public List<Item> getItemsOffered() {
        return itemsOffered;
    }

    public void setItemsOffered(List<Item> itemsOffered) {
        this.itemsOffered = itemsOffered;
    }

    // Phương thức hỗ trợ thêm nhanh sản phẩm vào danh sách của Seller
    public void addItem(Item item) {
        this.itemsOffered.add(item);
    }

    @Override
    public String toString() {
        return "Seller{" +
                "userId='" + getUserId() + '\'' +
                ", username='" + getUsername() + '\'' +
                ", rating=" + rating +
                ", itemsCount=" + itemsOffered.size() +
                '}';
    }
}