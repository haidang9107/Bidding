package repository; // Giữ nguyên cấu trúc thư mục hiện tại của bạn

import org.example.dto.response.PagedResponse;
import org.example.model.Auction;
import org.example.model.enums.AuctionStatus;
import org.example.model.product.Electronics;
import org.example.server.repository.AuctionDao;
import org.example.server.repository.ProductDao;
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

class AuctionDaoTest {

    // Thêm DROP ALL OBJECTS để dọn sạch DB cũ trước khi chạy test case tiếp theo
    private static final String H2_URL = "jdbc:h2:mem:bidding_test_db;DB_CLOSE_DELAY=-1;MODE=MySQL;INIT=DROP ALL OBJECTS\\;";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private Connection connection;
    private AuctionDao auctionDao;
    private ProductDao productDao;

    private int mockProductId1;
    private int mockProductId2;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection(H2_URL, USER, PASSWORD);
        importSchema();

        auctionDao = AuctionDao.getInstance();
        productDao = ProductDao.getInstance();

        insertMockUser("seller1", "Người Bán Một", "pass123", "seller1@gmail.com");
        insertMockUser("bidder1", "Người Đấu Giá Một", "pass456", "bidder1@gmail.com");

        mockProductId1 = insertMockProduct("Sản phẩm test 1", "seller1");
        mockProductId2 = insertMockProduct("Sản phẩm test 2", "seller1");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            // Thực hiện clear toàn bộ bảng tránh xung đột dữ liệu giữa các test case
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP ALL OBJECTS");
            }
            connection.close();
        }
    }

    @Test
    void testInsertAuction_Success() throws SQLException {
        Auction auction = new Auction();
        auction.setProductId(mockProductId1);
        auction.setSellerAccountname("seller1");
        auction.setStartingPrice(100000L);
        auction.setStepPrice(10000L);
        auction.setCurrentPrice(100000L);
        auction.setBuyNowPrice(500000L);
        auction.setStartTime(new Timestamp(System.currentTimeMillis()));
        auction.setEndTime(new Timestamp(System.currentTimeMillis() + 3600000));
        auction.setStatus(AuctionStatus.OPEN);

        boolean result = auctionDao.insertAuction(connection, auction);

        assertTrue(result);
        assertTrue(auction.getAuctionId() > 0);
    }

    @Test
    void testGetAuctionById_And_ForUpdate() throws SQLException {
        Auction auction = createAndInsertMockAuction(mockProductId1, "seller1", AuctionStatus.RUNNING, 100000L, 3600000);

        Auction dbAuction = auctionDao.getAuctionById(connection, auction.getAuctionId());
        assertNotNull(dbAuction);
        assertEquals("seller1", dbAuction.getSellerAccountname());

        Auction lockedAuction = auctionDao.getAuctionForUpdate(connection, auction.getAuctionId());
        assertNotNull(lockedAuction);
        assertEquals(dbAuction.getAuctionId(), lockedAuction.getAuctionId());
    }

    @Test
    void testGetAuctionsByStatusFilters() throws SQLException {
        createAndInsertMockAuction(mockProductId1, "seller1", AuctionStatus.RUNNING, 100000L, 3600000);

        Auction upcoming = createAndInsertMockAuction(mockProductId2, "seller1", AuctionStatus.OPEN, 200000L, 3600000);
        upcoming.setStartTime(new Timestamp(System.currentTimeMillis() - 60000));
        updateAuctionTimes(upcoming);

        List<Auction> runningList = auctionDao.getRunningAuctions(connection);
        assertEquals(1, runningList.size());

        List<Auction> upcomingList = auctionDao.getUpcomingAuctions(connection);
        assertEquals(1, upcomingList.size());
    }

    @Test
    void testGetExpiredAuctions() throws SQLException {
        createAndInsertMockAuction(mockProductId1, "seller1", AuctionStatus.RUNNING, 100000L, -60000);

        List<Auction> expiredList = auctionDao.getExpiredAuctions(connection);
        assertEquals(1, expiredList.size());
    }

    @Test
    void testGetAuctionsPagedResponse() throws SQLException {
        createAndInsertMockAuction(mockProductId1, "seller1", AuctionStatus.RUNNING, 100000L, 3600000);
        createAndInsertMockAuction(mockProductId2, "seller1", AuctionStatus.RUNNING, 150000L, 3600000);

        PagedResponse<Auction> response = auctionDao.getAuctionsPagedResponse(connection, 1, 1);

        assertNotNull(response);
        // Đã sửa đổi thành getTotal() và getItems() theo đúng cấu trúc thực tế class PagedResponse
        assertEquals(2, response.getTotal());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void testUpdateStatus_Success() throws SQLException {
        Auction auction = createAndInsertMockAuction(mockProductId1, "seller1", AuctionStatus.OPEN, 100000L, 3600000);

        boolean updated = auctionDao.updateStatus(connection, auction.getAuctionId(), AuctionStatus.FINISHED);

        assertTrue(updated);
        Auction dbAuction = auctionDao.getAuctionById(connection, auction.getAuctionId());
        assertEquals(AuctionStatus.FINISHED, dbAuction.getStatus());
    }

    @Test
    void testUpdateBidLocked_Success() throws SQLException {
        Auction auction = createAndInsertMockAuction(mockProductId1, "seller1", AuctionStatus.RUNNING, 100000L, 3600000);

        boolean updated = auctionDao.updateBidLocked(connection, auction.getAuctionId(), 120000L, "bidder1");

        assertTrue(updated);
        Auction dbAuction = auctionDao.getAuctionById(connection, auction.getAuctionId());
        assertEquals(120000L, dbAuction.getCurrentPrice());
        assertEquals("bidder1", dbAuction.getWinnerAccountname());
    }

    @Test
    void testUpdateAuctionEndTime_Success() throws SQLException {
        Auction auction = createAndInsertMockAuction(mockProductId1, "seller1", AuctionStatus.RUNNING, 100000L, 3600000);
        Timestamp newEndTime = new Timestamp(System.currentTimeMillis() + 7200000);

        boolean updated = auctionDao.updateAuctionEndTime(connection, auction.getAuctionId(), newEndTime);

        assertTrue(updated);
        Auction dbAuction = auctionDao.getAuctionById(connection, auction.getAuctionId());
        assertEquals(newEndTime.getTime(), dbAuction.getEndTime().getTime());
    }

    @Test
    void testIsUserLeadingAnyAuction() throws SQLException {
        Auction auction = createAndInsertMockAuction(mockProductId1, "seller1", AuctionStatus.RUNNING, 100000L, 3600000);

        assertFalse(auctionDao.isUserLeadingAnyAuction(connection, "bidder1"));

        auctionDao.updateBidLocked(connection, auction.getAuctionId(), 110000L, "bidder1");

        assertTrue(auctionDao.isUserLeadingAnyAuction(connection, "bidder1"));
    }

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

    private int insertMockProduct(String productName, String owner) throws SQLException {
        Electronics item = new Electronics();
        item.setName(productName);
        item.setOwnerAccountname(owner);
        item.setInAuction(true);
        productDao.insertProduct(connection, item);
        return item.getProductId();
    }

    private Auction createAndInsertMockAuction(int productId, String seller, AuctionStatus status, long price, long durationMs) throws SQLException {
        Auction auction = new Auction();
        auction.setProductId(productId);
        auction.setSellerAccountname(seller);
        auction.setStartingPrice(price);
        auction.setStepPrice(10000L);
        auction.setCurrentPrice(price);
        auction.setStartTime(new Timestamp(System.currentTimeMillis()));
        auction.setEndTime(new Timestamp(System.currentTimeMillis() + durationMs));
        auction.setStatus(status);

        auctionDao.insertAuction(connection, auction);
        return auction;
    }

    private void updateAuctionTimes(Auction auction) throws SQLException {
        String sql = "UPDATE auctions SET start_time = ?, end_time = ? WHERE auction_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, auction.getStartTime());
            ps.setTimestamp(2, auction.getEndTime());
            ps.setInt(3, auction.getAuctionId());
            ps.executeUpdate();
        }
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