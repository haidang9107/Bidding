package repository;

import org.example.model.AutoBid;
import org.example.server.repository.AutoBidDao;
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

class AutoBidDaoTest {

    private static final String H2_URL = "jdbc:h2:mem:bidding_test_db;DB_CLOSE_DELAY=-1;MODE=MySQL;INIT=DROP ALL OBJECTS\\;";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private Connection connection;
    private AutoBidDao autoBidDao;

    // Sử dụng ID đồng bộ để thỏa mãn các ràng buộc khóa ngoại (Foreign Key)
    private final int mockAuctionId = 1;
    private final int mockProductId = 1;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection(H2_URL, USER, PASSWORD);
        importSchema();

        autoBidDao = AutoBidDao.getInstance();

        // 1. Tạo dữ liệu User giả lập (Khớp hoàn toàn các ràng buộc không null)
        insertMockUser("seller1", "Người Bán", "pass123", "seller@gmail.com");
        insertMockUser("bidder1", "Người Đấu Giá 1", "pass456", "bidder1@gmail.com");
        insertMockUser("bidder2", "Người Đấu Giá 2", "pass789", "bidder2@gmail.com");

        // 2. Tạo dữ liệu Product giả lập (Category kiểu int = 1)
        insertMockProduct(mockProductId, "Sản phẩm AutoBid", "seller1", 1);

        // 3. Tạo cuộc đấu giá giả lập bằng hàm gán trực tiếp đủ 13 cột theo đúng cấu trúc schema.sql
        insertMockAuctionDirect(mockAuctionId, mockProductId, "seller1", 500000L);
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
    void testUpsertAutoBid_InsertSuccess() throws SQLException {
        boolean result = autoBidDao.upsertAutoBid(connection, mockAuctionId, "bidder1", 1000000L, 50000L);

        assertTrue(result);
        List<AutoBid> activeBids = autoBidDao.findAllActiveForAuction(connection, mockAuctionId);
        assertEquals(1, activeBids.size());
        assertEquals("bidder1", activeBids.get(0).getBidderAccountname());
        assertEquals(1000000L, activeBids.get(0).getMaxBid());
    }

    @Test
    void testUpsertAutoBid_UpdateExistingSuccess() throws SQLException {
        autoBidDao.upsertAutoBid(connection, mockAuctionId, "bidder1", 1000000L, 50000L);

        // Thực hiện cập nhật cấu hình tự động đấu giá hiện có
        boolean result = autoBidDao.upsertAutoBid(connection, mockAuctionId, "bidder1", 1500000L, 100000L);

        assertTrue(result);
        List<AutoBid> activeBids = autoBidDao.findAllActiveForAuction(connection, mockAuctionId);
        assertEquals(1, activeBids.size());
        assertEquals(1500000L, activeBids.get(0).getMaxBid());
    }

    @Test
    void testDeactivateAutoBid_Success() throws SQLException {
        autoBidDao.upsertAutoBid(connection, mockAuctionId, "bidder1", 1000000L, 50000L);

        boolean deactivated = autoBidDao.deactivateAutoBid(connection, mockAuctionId, "bidder1");

        assertTrue(deactivated);
        List<AutoBid> activeBids = autoBidDao.findAllActiveForAuction(connection, mockAuctionId);
        assertTrue(activeBids.isEmpty());
    }

    @Test
    void testFindAllActiveForAuction_OrderedByMaxBidDesc() throws SQLException {
        autoBidDao.upsertAutoBid(connection, mockAuctionId, "bidder1", 800000L, 20000L);
        autoBidDao.upsertAutoBid(connection, mockAuctionId, "bidder2", 1200000L, 50000L);

        List<AutoBid> activeBids = autoBidDao.findAllActiveForAuction(connection, mockAuctionId);

        assertNotNull(activeBids);
        assertEquals(2, activeBids.size());
        // Sắp xếp theo max_bid DESC nên bidder2 (1.200.000) phải đứng trước bidder1 (800.000)
        assertEquals("bidder2", activeBids.get(0).getBidderAccountname());
        assertEquals(1200000L, activeBids.get(0).getMaxBid());
    }

    // ==================================================
    // CÁC HÀM TRỢ GIÚP INSERT MOCK DATA TUYỆT ĐỐI AN TOÀN
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

    private void insertMockAuctionDirect(int auctionId, int productId, String seller, long price) throws SQLException {
        // Đã sửa đổi cấu trúc: Chèn trực tiếp không liệt kê cột, gán đủ và đúng thứ tự 13 tham số (?)
        // dựa theo cấu trúc định nghĩa chính xác từ file schema.sql của bạn.
        String sql = "INSERT INTO auctions VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);                                      // auction_id
            ps.setInt(2, productId);                                      // product_id
            ps.setString(3, seller);                                      // seller_accountname
            ps.setNull(4, java.sql.Types.VARCHAR);                        // winner_accountname (mặc định chưa có ai thắng)
            ps.setLong(5, price);                                         // start_price (khớp tên start_price trong schema)
            ps.setLong(6, 10000L);                                        // step_price
            ps.setLong(7, price);                                         // current_price
            ps.setNull(8, java.sql.Types.BIGINT);                         // buy_now_price
            ps.setTimestamp(9, new Timestamp(System.currentTimeMillis())); // start_time
            ps.setTimestamp(10, new Timestamp(System.currentTimeMillis() + 3600000)); // end_time
            ps.setInt(11, 0);                                             // status (kiểu INT: 0 biểu thị đang chạy)
            ps.setInt(12, 0);                                             // version
            ps.setTimestamp(13, new Timestamp(System.currentTimeMillis())); // created_at
            ps.executeUpdate();
        }
    }

    private void importSchema() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("Không tìm thấy file schema.sql!");
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