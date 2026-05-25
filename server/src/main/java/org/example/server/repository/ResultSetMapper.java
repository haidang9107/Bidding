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
 * Utility class to map ResultSet rows to domain models.
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
     * Maps a ResultSet row (from the {@code products} table or a JOIN that includes
     * its columns) to the appropriate {@link Product} subclass.
     * @param rs The ResultSet.
     * @return The mapped Product object.
     * @throws SQLException If a database error occurs.
     */
    public static Product mapToProduct(ResultSet rs) throws SQLException {
        int productId = rs.getInt("product_id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        String imageUrl = rs.getString("image_url");
        ItemCategory category = ItemCategory.fromInt(rs.getInt("category"));
        String ownerAccountname = rs.getString("owner_accountname");

        Product product;
        if (category == ItemCategory.ELECTRONICS) {
            product = new Electronics(productId, name, description, imageUrl, ownerAccountname,
                    rs.getString("brand"), rs.getInt("warranty_months"));
        } else if (category == ItemCategory.ART) {
            product = new Art(productId, name, description, imageUrl, ownerAccountname,
                    rs.getString("artist"), rs.getString("art_type"));
        } else if (category == ItemCategory.VEHICLE) {
            product = new Vehicle(productId, name, description, imageUrl, ownerAccountname,
                    rs.getString("brand"), rs.getString("model"), rs.getInt("manufacture_year"));
        } else {
            product = new OtherItem(productId, name, description, imageUrl, ownerAccountname);
        }

        product.setInAuction(rs.getBoolean("is_in_auction"));
        product.setWithdrawnAt(rs.getTimestamp("withdrawn_at"));
        return product;
    }

    /**
     * Maps a ResultSet row from the {@code auctions} table to an {@link Auction}.
     * Does NOT populate the embedded {@link Product}; use {@link #mapToAuctionWithProduct}
     * when the query joins {@code products}.
     * @param rs The ResultSet.
     * @return The mapped Auction.
     * @throws SQLException If a database error occurs.
     */
    public static Auction mapToAuction(ResultSet rs) throws SQLException {
        long buyNowPrice = rs.getLong("buy_now_price");
        Long buyNow = rs.wasNull() ? null : buyNowPrice;

        return new Auction(
                rs.getInt("auction_id"),
                rs.getInt("product_id"),
                rs.getString("seller_accountname"),
                rs.getString("winner_accountname"),
                rs.getLong("start_price"),
                rs.getLong("current_price"),
                rs.getLong("step_price"),
                buyNow,
                rs.getTimestamp("start_time"),
                rs.getTimestamp("end_time"),
                AuctionStatus.fromInt(rs.getInt("status")),
                rs.getInt("version")
        );
    }

    /**
     * Maps a ResultSet row from a JOIN of {@code auctions} and {@code products} to an
     * {@link Auction} with its {@link Product} eagerly loaded.
     * @param rs The ResultSet.
     * @return The mapped Auction with its product.
     * @throws SQLException If a database error occurs.
     */
    public static Auction mapToAuctionWithProduct(ResultSet rs) throws SQLException {
        Auction auction = mapToAuction(rs);
        auction.setProduct(mapToProduct(rs));
        return auction;
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
        Integer productIdOrNull = rs.wasNull() ? null : productId;
        int auctionId = rs.getInt("auction_id");
        Integer auctionIdOrNull = rs.wasNull() ? null : auctionId;
        Timestamp createdAt = rs.getTimestamp("created_at");

        return new Transaction(
                rs.getInt("transaction_id"),
                rs.getString("sender_accountname"),
                rs.getString("receiver_accountname"),
                TransactionType.fromInt(rs.getInt("type")),
                productIdOrNull,
                rs.getLong("amount"),
                auctionIdOrNull,
                rs.getString("description"),
                createdAt
        );
    }
}
