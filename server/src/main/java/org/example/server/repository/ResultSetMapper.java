package org.example.server.repository;

import org.example.model.Auction;
import org.example.model.AutoBid;
import org.example.model.Bid;
import org.example.model.Transaction;
import org.example.model.enums.AuctionStatus;
import org.example.model.enums.ItemCategory;
import org.example.model.enums.TransactionType;
import org.example.model.enums.UserRole;
import org.example.model.product.*;
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

    /**
     * Maps a ResultSet row to a User object (either Admin or Member).
     * @param rs The ResultSet.
     * @return The mapped User object.
     * @throws SQLException If a database error occurs.
     */
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

    /**
     * Maps a ResultSet row to an Item object of the appropriate subclass (Art, Electronics, etc.).
     * @param rs The ResultSet.
     * @return The mapped Item object.
     * @throws SQLException If a database error occurs.
     */
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
        } else if (category == ItemCategory.VEHICLE) {
            item = new Vehicle(productId, name, description, imageUrl, startingPrice, currentPrice,
                    stepPrice, sellerAccountname, winnerAccountname, status, startTime, endTime,
                    version, rs.getString("brand"), rs.getString("model"), rs.getInt("manufacture_year"));
        } else {
            item = new OtherItem(productId, name, description, imageUrl, startingPrice, currentPrice,
                    stepPrice, sellerAccountname, winnerAccountname, status, startTime, endTime, version);
        }

        item.setAuctionId(rs.getInt("auction_id"));
        item.setOwnerAccountname(rs.getString("owner_accountname"));
        item.setInAuction(rs.getBoolean("is_in_auction"));
        item.setWithdrawnAt(rs.getTimestamp("withdrawn_at"));
        
        long buyNowPrice = rs.getLong("buy_now_price");
        item.setBuyNowPrice(rs.wasNull() ? null : buyNowPrice);
        
        return item;
    }

    /**
     * Maps a ResultSet row to an Auction object.
     * @param rs The ResultSet.
     * @return The mapped Auction object.
     * @throws SQLException If a database error occurs.
     */
    public static Auction mapToAuction(ResultSet rs) throws SQLException {
        return new Auction(
                rs.getInt("bid_id"),
                rs.getInt("auction_id"),
                rs.getString("bidder_accountname"),
                rs.getLong("bid_amount"),
                rs.getTimestamp("bid_time")
        );
    }

    /**
     * Maps a ResultSet row to a Bid object.
     * @param rs The ResultSet.
     * @return The mapped Bid object.
     * @throws SQLException If a database error occurs.
     */
    public static Bid mapToBid(ResultSet rs) throws SQLException {
        return new Bid(
                rs.getInt("auction_id"),
                rs.getString("bidder_accountname"),
                rs.getLong("bid_amount"),
                rs.getTimestamp("bid_time")
        );
    }

    /**
     * Maps a ResultSet row to an AutoBid object.
     * @param rs The ResultSet.
     * @return The mapped AutoBid object.
     * @throws SQLException If a database error occurs.
     */
    public static AutoBid mapToAutoBid(ResultSet rs) throws SQLException {
        return new AutoBid(
                rs.getInt("auto_bid_id"),
                rs.getInt("auction_id"),
                rs.getString("bidder_accountname"),
                rs.getLong("max_bid"),
                rs.getLong("increment_amount"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")
        );
    }

    /**
     * Maps a ResultSet row to a Transaction object.
     * @param rs The ResultSet.
     * @return The mapped Transaction object.
     * @throws SQLException If a database error occurs.
     */
    public static Transaction mapToTransaction(ResultSet rs) throws SQLException {
        int productId = rs.getInt("product_id");
        int referenceId = rs.getInt("reference_id");
        
        return new Transaction(
                rs.getInt("transaction_id"),
                rs.getString("sender_accountname"),
                rs.getString("receiver_accountname"),
                TransactionType.fromInt(rs.getInt("type")),
                rs.wasNull() ? null : productId,
                rs.getLong("amount"),
                rs.wasNull() ? null : referenceId,
                rs.getString("description"),
                rs.getTimestamp("created_at")
        );
    }
}
