package org.example.server.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể đại diện cho Người đấu giá (Bidder).
 * Kế thừa từ User và gián tiếp kế thừa từ Entity.
 */
public class Bidder extends User {

    // Danh sách các lượt đặt giá mà người này đã thực hiện
    private List<Auction> bidHistory;

    public Bidder() {
        super();
        this.setRole("USER"); // Hoặc "BIDDER" tùy theo cách bạn định nghĩa role trong DB
        this.bidHistory = new ArrayList<>();
    }

    /**
     * Constructor đầy đủ để lấy dữ liệu từ database
     */
    public Bidder(String userId, String username, String password, String email,
                  String phoneNumber, String gender, String avt,
                  BigDecimal balance, Timestamp createdAt) {

        super(userId, username, password, email, phoneNumber, gender, avt, balance, "USER", createdAt);
        this.bidHistory = new ArrayList<>();
    }

    // --- Getter và Setter ---

    public List<Auction> getBidHistory() {
        return bidHistory;
    }

    public void setBidHistory(List<Auction> bidHistory) {
        this.bidHistory = bidHistory;
    }

    /**
     * Phương thức hỗ trợ thêm một lượt đấu giá mới vào lịch sử của người dùng
     */
    public void addBid(Auction auction) {
        this.bidHistory.add(auction);
    }

    @Override
    public String toString() {
        return "Bidder{" +
                "userId='" + getUserId() + '\'' +
                ", username='" + getUsername() + '\'' +
                ", balance=" + getBalance() +
                ", totalBids=" + bidHistory.size() +
                '}';
    }
}