package org.example.server.repository;

import org.example.server.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDao {

    // =========================
    // Connection
    // =========================
    private Connection connection;

    // =========================
    // Constructor
    // =========================
    public AuctionDao(Connection connection) {
        this.connection = connection;
    }

    // =========================
    // Lấy toàn bộ bids/auctions
    // =========================
    public List<Bid> getAllAuctions()
            throws SQLException {

        List<Bid> bids =
                new ArrayList<>();

        String sql = """
                SELECT a.*,
                
                       p.product_id,
                       p.product_name,
                       p.description,
                       p.starting_price,
                       p.step_price,
                       p.category,
                       p.status,
                       p.created_at AS product_created_at,
                
                       s.user_id AS seller_id,
                       s.username AS seller_username,
                       s.password AS seller_password,
                       s.email AS seller_email,
                       s.phonenumber AS seller_phone,
                       s.gender AS seller_gender,
                       s.avt AS seller_avt,
                       s.balance AS seller_balance,
                       s.created_at AS seller_created_at,
                
                       b.user_id AS bidder_id,
                       b.username AS bidder_username,
                       b.password AS bidder_password,
                       b.email AS bidder_email,
                       b.phonenumber AS bidder_phone,
                       b.gender AS bidder_gender,
                       b.avt AS bidder_avt,
                       b.balance AS bidder_balance,
                       b.created_at AS bidder_created_at
                
                FROM auctions a
                
                JOIN products p
                ON a.product_id = p.product_id
                
                JOIN users s
                ON p.seller_id = s.user_id
                
                JOIN users b
                ON a.bidder_id = b.user_id
                """;

        try (
                Statement stmt =
                        connection.createStatement();

                ResultSet rs =
                        stmt.executeQuery(sql)
        ) {

            while (rs.next()) {

                // =========================
                // Seller
                // =========================
                Seller seller = new Seller(
                        rs.getString("seller_id"),
                        rs.getString("seller_username"),
                        rs.getString("seller_password"),
                        rs.getString("seller_email"),
                        rs.getString("seller_phone"),
                        rs.getString("seller_gender"),
                        rs.getString("seller_avt"),
                        rs.getDouble("seller_balance"),
                        rs.getTimestamp(
                                "seller_created_at"
                        )
                );

                // =========================
                // Item theo category
                // =========================
                String category =
                        rs.getString("category");

                Item item;

                if (category.equalsIgnoreCase(
                        "Electronics")) {

                    item = new Electronics(
                            rs.getString("product_id"),
                            rs.getString("product_name"),
                            rs.getString("description"),
                            rs.getDouble("starting_price"),
                            rs.getDouble("step_price"),
                            seller,
                            rs.getString("status"),
                            rs.getTimestamp(
                                    "product_created_at"
                            )
                    );

                } else if (category.equalsIgnoreCase(
                        "Art")) {

                    item = new Art(
                            rs.getString("product_id"),
                            rs.getString("product_name"),
                            rs.getString("description"),
                            rs.getDouble("starting_price"),
                            rs.getDouble("step_price"),
                            seller,
                            rs.getString("status"),
                            rs.getTimestamp(
                                    "product_created_at"
                            )
                    );

                } else {

                    item = new Vehicle(
                            rs.getString("product_id"),
                            rs.getString("product_name"),
                            rs.getString("description"),
                            rs.getDouble("starting_price"),
                            rs.getDouble("step_price"),
                            seller,
                            rs.getString("status"),
                            rs.getTimestamp(
                                    "product_created_at"
                            )
                    );
                }

                // =========================
                // Bidder
                // =========================
                Bidder bidder = new Bidder(
                        rs.getString("bidder_id"),
                        rs.getString("bidder_username"),
                        rs.getString("bidder_password"),
                        rs.getString("bidder_email"),
                        rs.getString("bidder_phone"),
                        rs.getString("bidder_gender"),
                        rs.getString("bidder_avt"),
                        rs.getDouble("bidder_balance"),
                        rs.getTimestamp(
                                "bidder_created_at"
                        )
                );

                // =========================
                // Bid object
                // =========================
                Bid bid = new Bid(
                        rs.getString("auction_id"),
                        bidder,
                        item,
                        rs.getDouble("bid_amount"),
                        rs.getTimestamp("bid_time")
                );

                bids.add(bid);
            }
        }

        return bids;
    }

    // =========================
    // Insert bid
    // =========================
    public void insertAuction(Bid bid)
            throws SQLException {

        String sql = """
                INSERT INTO auctions(
                    auction_id,
                    product_id,
                    bidder_id,
                    bid_amount,
                    bid_time
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    bid.getBidId()
            );

            ps.setString(
                    2,
                    bid.getItem().getProductId()
            );

            ps.setString(
                    3,
                    bid.getBidder().getUserId()
            );

            ps.setDouble(
                    4,
                    bid.getBidAmount()
            );

            ps.setTimestamp(
                    5,
                    bid.getBidTime()
            );

            ps.executeUpdate();
        }
    }

    // =========================
    // Xóa auction
    // =========================
    public void deleteAuction(String auctionId)
            throws SQLException {

        String sql = """
                DELETE FROM auctions
                WHERE auction_id = ?
                """;

        try (
                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(1, auctionId);

            ps.executeUpdate();
        }
    }

    // =========================
    // Tìm auction theo ID
    // =========================
    public Bid getAuctionById(String auctionId)
            throws SQLException {

        String sql = """
                SELECT a.*,
                
                       p.product_id,
                       p.product_name,
                       p.description,
                       p.starting_price,
                       p.step_price,
                       p.category,
                       p.status,
                       p.created_at AS product_created_at,
                
                       s.user_id AS seller_id,
                       s.username AS seller_username,
                       s.password AS seller_password,
                       s.email AS seller_email,
                       s.phonenumber AS seller_phone,
                       s.gender AS seller_gender,
                       s.avt AS seller_avt,
                       s.balance AS seller_balance,
                       s.created_at AS seller_created_at,
                
                       b.user_id AS bidder_id,
                       b.username AS bidder_username,
                       b.password AS bidder_password,
                       b.email AS bidder_email,
                       b.phonenumber AS bidder_phone,
                       b.gender AS bidder_gender,
                       b.avt AS bidder_avt,
                       b.balance AS bidder_balance,
                       b.created_at AS bidder_created_at
                
                FROM auctions a
                
                JOIN products p
                ON a.product_id = p.product_id
                
                JOIN users s
                ON p.seller_id = s.user_id
                
                JOIN users b
                ON a.bidder_id = b.user_id
                
                WHERE a.auction_id = ?
                """;

        try (
                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(1, auctionId);

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                if (rs.next()) {

                    Seller seller = new Seller(
                            rs.getString("seller_id"),
                            rs.getString("seller_username"),
                            rs.getString("seller_password"),
                            rs.getString("seller_email"),
                            rs.getString("seller_phone"),
                            rs.getString("seller_gender"),
                            rs.getString("seller_avt"),
                            rs.getDouble("seller_balance"),
                            rs.getTimestamp(
                                    "seller_created_at"
                            )
                    );

                    String category =
                            rs.getString("category");

                    Item item;

                    if (category.equalsIgnoreCase(
                            "Electronics")) {

                        item = new Electronics(
                                rs.getString("product_id"),
                                rs.getString("product_name"),
                                rs.getString("description"),
                                rs.getDouble("starting_price"),
                                rs.getDouble("step_price"),
                                seller,
                                rs.getString("status"),
                                rs.getTimestamp(
                                        "product_created_at"
                                )
                        );

                    } else if (category.equalsIgnoreCase(
                            "Art")) {

                        item = new Art(
                                rs.getString("product_id"),
                                rs.getString("product_name"),
                                rs.getString("description"),
                                rs.getDouble("starting_price"),
                                rs.getDouble("step_price"),
                                seller,
                                rs.getString("status"),
                                rs.getTimestamp(
                                        "product_created_at"
                                )
                        );

                    } else {

                        item = new Vehicle(
                                rs.getString("product_id"),
                                rs.getString("product_name"),
                                rs.getString("description"),
                                rs.getDouble("starting_price"),
                                rs.getDouble("step_price"),
                                seller,
                                rs.getString("status"),
                                rs.getTimestamp(
                                        "product_created_at"
                                )
                        );
                    }

                    Bidder bidder = new Bidder(
                            rs.getString("bidder_id"),
                            rs.getString("bidder_username"),
                            rs.getString("bidder_password"),
                            rs.getString("bidder_email"),
                            rs.getString("bidder_phone"),
                            rs.getString("bidder_gender"),
                            rs.getString("bidder_avt"),
                            rs.getDouble("bidder_balance"),
                            rs.getTimestamp(
                                    "bidder_created_at"
                            )
                    );

                    return new Bid(
                            rs.getString("auction_id"),
                            bidder,
                            item,
                            rs.getDouble("bid_amount"),
                            rs.getTimestamp("bid_time")
                    );
                }
            }
        }

        return null;
    }
}