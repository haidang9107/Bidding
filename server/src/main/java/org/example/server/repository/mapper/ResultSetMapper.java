package org.example.server.repository.mapper;

import org.example.model.enums.AuctionStatus;
import org.example.model.enums.ItemCategory;
import org.example.model.enums.UserRole;
import org.example.model.product.Art;
import org.example.model.product.Electronics;
import org.example.model.product.Item;
import org.example.model.product.Vehicle;
import org.example.model.user.Admin;
import org.example.model.user.Member;
import org.example.model.user.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Utility class to map ResultSet rows to Domain Models.
 * Follows SRP by separating mapping logic from DAO database operations.
 */
public class ResultSetMapper {

    public static User mapToUser(ResultSet rs) throws SQLException {
        int roleValue = rs.getInt("role");
        UserRole role = UserRole.fromInt(roleValue);
        
        String accountname = rs.getString("accountname");
        String password = rs.getString("password");
        String fullname = rs.getString("fullname");
        String email = rs.getString("email");
        String avt = rs.getString("avt");
        long balance = rs.getLong("balance");
        long blockedBalance = rs.getLong("blocked_balance");
        int status = rs.getInt("status");

        User user;
        if (role == UserRole.ADMIN) {
            Admin admin = new Admin(accountname, password, email, avt, status);
            admin.setFullname(fullname);
            user = admin;
        } else {
            Member member = new Member(accountname, password, email, avt, status, balance, blockedBalance);
            member.setFullname(fullname);
            user = member;
        }
        return user;
    }

    public static Item mapToItem(ResultSet rs) throws SQLException {
        int productId = rs.getInt("product_id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        String imageUrl = rs.getString("image_url");
        long startingPrice = rs.getLong("start_price");
        long currentPrice = rs.getLong("current_price");
        long stepPrice = rs.getLong("step_price");
        String sellerAccountname = rs.getString("seller_accountname");
        String winnerAccountname = rs.getString("winner_accountname");
        ItemCategory category = ItemCategory.fromInt(rs.getInt("category"));
        AuctionStatus status = AuctionStatus.fromInt(rs.getInt("status"));
        Timestamp startTime = rs.getTimestamp("start_time");
        Timestamp endTime = rs.getTimestamp("end_time");
        int version = rs.getInt("version");

        Item item;
        if (category == ItemCategory.ELECTRONICS) {
            item = new Electronics(productId, name, description, imageUrl, startingPrice, currentPrice,
                    stepPrice, sellerAccountname, winnerAccountname, status, startTime, endTime,
                    version, rs.getString("brand"), rs.getInt("warranty_months"));
        } else if (category == ItemCategory.ART) {
            item = new Art(productId, name, description, imageUrl, startingPrice, currentPrice,
                    stepPrice, sellerAccountname, winnerAccountname, status, startTime, endTime,
                    version, rs.getString("artist"), rs.getString("art_type"));
        } else {
            item = new Vehicle(productId, name, description, imageUrl, startingPrice, currentPrice,
                    stepPrice, sellerAccountname, winnerAccountname, status, startTime, endTime,
                    version, rs.getString("brand"), rs.getString("model"), rs.getInt("manufacture_year"));
        }

        item.setAuctionId(rs.getInt("auction_id"));
        item.setOwnerAccountname(rs.getString("owner_accountname"));
        item.setInAuction(rs.getBoolean("is_in_auction"));
        item.setWithdrawnAt(rs.getTimestamp("withdrawn_at"));
        
        long buyNowPrice = rs.getLong("buy_now_price");
        item.setBuyNowPrice(rs.wasNull() ? null : buyNowPrice);
        
        return item;
    }
}
