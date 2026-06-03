package repository;

import org.example.model.Auction;
import org.example.model.Bid;
import org.example.model.enums.AuctionStatus;
import org.example.server.repository.AuctionDao;
import org.example.server.repository.BidDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class BidDaoTest {

    private static final String H2_URL = "jdbc:h2:mem:bidding_test_db;DB_CLOSE_DELAY=-1;MODE=MySQL;INIT=DROP ALL OBJECTS\\;";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private Connection connection;
    private BidDao bidDao;
    private AuctionDao auctionDao; // Thêm AuctionDao để insert mock dữ liệu chuẩn theo hệ thống

    private final int mockAuctionId = 1;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection(H2_URL, USER, PASSWORD);
        importSchema();

        bidDao = BidDao.getInstance();
        auctionDao = AuctionDao.getInstance();

        // Tạo dữ liệu giả lập cho User và Product
        insertMockUser("seller1", "Người Bán", "pass123", "seller@gmail.com");
        insertMockUser("bidder1", "Người Đấu Giá Một", "pass456", "bidder1@gmail.com");
        insertMockUser("bidder2", "Người Đấu Giá Hai", "pass789", "bidder2@gmail.com");

        insertMockProduct(1, "Sản phẩm test", "seller1", 1);

        // Gọi hàm mock đấu giá bằng cách dùng chính AuctionDao để không lo sai tên cột SQL
        insertMockAuctionViaDao(mockAuctionId, 1, "seller1", 100000L);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP ALL OBJECTS");
            }
            connection.close();
        }
    }

    @Test
    void testInsertBid_Success() throws SQLException {
        boolean result = bidDao.insertBid(connection, mockAuctionId, "bidder1", 110000L, false);

        assertTrue(result);
        assertEquals(1, bidDao.getTotalBidsCount(connection, mockAuctionId));
    }

    @Test
    void testGetBidsForDisplay_OrderedByAmountDesc() throws SQLException {
        bidDao.insertBid(connection, mockAuctionId, "bidder1", 110000L, false);
        bidDao.insertBid(connection, mockAuctionId, "bidder2", 130000L, false);
        bidDao.insertBid(connection, mockAuctionId, "bidder1", 120000L, true);

        List<Bid> bids = bidDao.getBidsForDisplay(connection, mockAuctionId);

        assertNotNull(bids);
        assertEquals(3, bids.size());
        assertEquals(130000L, bids.get(0).getBidAmount());
        assertEquals("bidder2", bids.get(0).getBidderAccountname());
        assertEquals(120000L, bids.get(1).getBidAmount());
    }

    @Test
    void testGetBidsPaged_Success() throws SQLException {
        bidDao.insertBid(connection, mockAuctionId, "bidder1", 110000L, false);
        bidDao.insertBid(connection, mockAuctionId, "bidder2", 120000L, false);
        bidDao.insertBid(connection, mockAuctionId, "bidder1", 130000L, false);

        List<Bid> page1 = bidDao.getBidsPaged(connection, mockAuctionId, 2, 0);
        assertNotNull(page1);
        assertEquals(2, page1.size());
        assertEquals(130000L, page1.get(0).getBidAmount());

        List<Bid> page2 = bidDao.getBidsPaged(connection, mockAuctionId, 2, 2);
        assertNotNull(page2);
        assertEquals(1, page2.size());
        assertEquals(120000L, page2.get(0).getBidAmount());
    }

    @Test
    void testGetTotalBidsCount_ReturnsCorrectCount() throws SQLException {
        assertEquals(0, bidDao.getTotalBidsCount(connection, mockAuctionId));

        bidDao.insertBid(connection, mockAuctionId, "bidder1", 110000L, false);
        bidDao.insertBid(connection, mockAuctionId, "bidder2", 120000L, true);

        long totalCount = bidDao.getTotalBidsCount(connection, mockAuctionId);
        assertEquals(2, totalCount);
    }

    // ==================================================
    // HÀM TRỢ GIÚP TẠO MOCK DATA SỬ DỤNG DAO CỦA HỆ THỐNG
    // ==================================================

    private void insertMockUser(String username, String fullname, String password, String email) throws SQLException {
        String sql = "INSERT INTO users(accountname, fullname, password, email, role) VALUES (?, ?, ?, ?, 1)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, fullname);
            ps.setString(3, password);
            ps.setString(4, email);
            ps.executeUpdate();
        }
    }

    private void insertMockProduct(int productId, String productName, String owner, int category) throws SQLException {
        String sql = "INSERT INTO products(product_id, name, owner_accountname, is_in_auction, category) VALUES (?, ?, ?, true, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setString(2, productName);
            ps.setString(3, owner);
            ps.setInt(4, category);
            ps.executeUpdate();
        }
    }

    private void insertMockAuctionViaDao(int auctionId, int productId, String seller, long price) throws SQLException {
        Auction auction = new Auction();
        auction.setAuctionId(auctionId); // Gán cứng ID để đồng bộ với mockAuctionId trong test case
        auction.setProductId(productId);
        auction.setSellerAccountname(seller);
        auction.setStartingPrice(price);
        auction.setStepPrice(10000L);
        auction.setCurrentPrice(price);
        auction.setStartTime(new Timestamp(System.currentTimeMillis()));
        auction.setEndTime(new Timestamp(System.currentTimeMillis() + 3600000));
        auction.setStatus(AuctionStatus.RUNNING);

        // Dùng luôn hàm insert chuẩn của AuctionDao để tránh lỗi gõ sai tên cột SQL
        auctionDao.insertAuction(connection, auction);
    }

    private void importSchema() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("Không tìm thấy file schema.sql trong folder test resources!");
            }
            try (Scanner scanner = new Scanner(is).useDelimiter(";")) {
                try (Statement stmt = connection.createStatement()) {
                    while (scanner.hasNext()) {
                        String sql = scanner.next().trim();
                        if (!sql.isEmpty()) {
                            stmt.execute(sql);
                        }
                    }
                }
            }
        }
    }
}