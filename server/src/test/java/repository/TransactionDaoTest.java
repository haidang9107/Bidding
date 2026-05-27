package repository;

import org.example.model.Transaction;
import org.example.model.enums.TransactionType;
import org.example.server.repository.TransactionDao;
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

class TransactionDaoTest {

    private static final String H2_URL = "jdbc:h2:mem:bidding_test_db;DB_CLOSE_DELAY=-1;MODE=MySQL;INIT=DROP ALL OBJECTS\\;";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private Connection connection;
    private TransactionDao transactionDao;

    // Gán cứng ID đồng bộ để kiểm soát chặt chẽ ràng buộc khóa ngoại
    private final int mockProductId = 1;
    private final int mockAuctionId = 1;

    // Giả lập Enum TransactionType nếu cấu trúc thực tế của bạn có các trường tương tự
    // (Đảm bảo truyền đúng loại Enum từ gói org.example.model.enums.TransactionType của dự án)
    private final TransactionType mockType = TransactionType.DEPOSIT;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection(H2_URL, USER, PASSWORD);
        importSchema();

        transactionDao = TransactionDao.getInstance();

        // 1. Tạo dữ liệu người dùng mồi (Đầy đủ cả người gửi và người nhận)
        insertMockUser("user_sender", "Người Gửi", "pass1", "sender@gmail.com");
        insertMockUser("user_receiver", "Người Nhận", "pass2", "receiver@gmail.com");

        // 2. Tạo sản phẩm mồi (Gán category = 1 để tránh lỗi loại dữ liệu / NULL)
        insertMockProduct(mockProductId, "Sản phẩm Đấu Giá", "user_sender", 1);

        // 3. Tạo cuộc đấu giá mồi bằng hàm trực tiếp 13 cột (Bypass lỗi Column count)
        insertMockAuctionDirect(mockAuctionId, mockProductId, "user_sender", 200000L);
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
    void testInsertTransaction_Success_FullData() throws SQLException {
        boolean result = transactionDao.insertTransaction(
                connection, "user_sender", "user_receiver", mockType, mockProductId, 150000L, mockAuctionId, "Thanh toán thành công"
        );

        assertTrue(result);
        List<Transaction> transactions = transactionDao.getTransactionsByUser(connection, "user_sender");
        assertEquals(1, transactions.size());
        assertEquals("user_sender", transactions.get(0).getSenderAccountname());
        assertEquals("user_receiver", transactions.get(0).getReceiverAccountname());
        assertEquals(150000L, transactions.get(0).getAmount());
    }

    @Test
    void testInsertTransaction_Success_NullOptionalFields() throws SQLException {
        // Kiểm tra tính đúng đắn khi các trường tùy chọn (như product_id, auction_id) nhận giá trị NULL
        boolean result = transactionDao.insertTransaction(
                connection, "user_sender", null, mockType, null, 50000L, null, "Nạp tiền hệ thống"
        );

        assertTrue(result);
        List<Transaction> transactions = transactionDao.getTransactionsByUser(connection, "user_sender");
        assertEquals(1, transactions.size());
        assertNull(transactions.get(0).getReceiverAccountname());
        assertNull(transactions.get(0).getProductId());
        assertNull(transactions.get(0).getAuctionId());
    }

    @Test
    void testGetTransactionsByUser_FilterCorrectly() throws SQLException {
        // Tạo 3 giao dịch: 2 liên quan tới 'user_sender', 1 giao dịch độc lập không liên quan
        transactionDao.insertTransaction(connection, "user_sender", "user_receiver", mockType, null, 100L, null, "Tx1");
        transactionDao.insertTransaction(connection, "user_receiver", "user_sender", mockType, null, 200L, null, "Tx2");

        insertMockUser("other_user", "Khách Ngoài", "p3", "other@gmail.com");
        transactionDao.insertTransaction(connection, "user_receiver", "other_user", mockType, null, 300L, null, "Tx3");

        // Lấy danh sách giao dịch của user_sender
        List<Transaction> list = transactionDao.getTransactionsByUser(connection, "user_sender");

        assertEquals(2, list.size(), "Chỉ lấy các giao dịch mà user_sender làm người gửi hoặc người nhận");
    }

    @Test
    void testGetTransactionsPaged_PaginationCorrect() throws SQLException {
        // Chèn liên tiếp 3 giao dịch
        transactionDao.insertTransaction(connection, "user_sender", "user_receiver", mockType, null, 1000L, null, "Tx 1");
        transactionDao.insertTransaction(connection, "user_sender", "user_receiver", mockType, null, 2000L, null, "Tx 2");
        transactionDao.insertTransaction(connection, "user_sender", "user_receiver", mockType, null, 3000L, null, "Tx 3");

        // Lấy trang đầu tiên: lấy tối đa 2 bản ghi, bỏ qua 0 bản ghi (Trang 1)
        List<Transaction> page1 = transactionDao.getTransactionsPaged(connection, "user_sender", 2, 0);
        assertEquals(2, page1.size());

        // Lấy trang tiếp theo: lấy tối đa 2 bản ghi, bỏ qua 2 bản ghi trước đó (Trang 2)
        List<Transaction> page2 = transactionDao.getTransactionsPaged(connection, "user_sender", 2, 2);
        assertEquals(1, page2.size());
    }

    @Test
    void testGetTotalTransactionsCount() throws SQLException {
        transactionDao.insertTransaction(connection, "user_sender", "user_receiver", mockType, null, 500L, null, "Dữ liệu 1");
        transactionDao.insertTransaction(connection, "user_receiver", "user_sender", mockType, null, 600L, null, "Dữ liệu 2");

        long count = transactionDao.getTotalTransactionsCount(connection, "user_sender");

        assertEquals(2, count);
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
        // Áp dụng định vị trực tiếp 13 cột để loại bỏ lỗi lệch cấu hình dữ liệu H2
        String sql = "INSERT INTO auctions VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);                                      // auction_id
            ps.setInt(2, productId);                                      // product_id
            ps.setString(3, seller);                                      // seller_accountname
            ps.setNull(4, java.sql.Types.VARCHAR);                        // winner_accountname
            ps.setLong(5, price);                                         // start_price
            ps.setLong(6, 5000L);                                         // step_price
            ps.setLong(7, price);                                         // current_price
            ps.setNull(8, java.sql.Types.BIGINT);                         // buy_now_price
            ps.setTimestamp(9, new Timestamp(System.currentTimeMillis())); // start_time
            ps.setTimestamp(10, new Timestamp(System.currentTimeMillis() + 3600000)); // end_time
            ps.setInt(11, 0);                                             // status (INT)
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