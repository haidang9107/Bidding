package repository;

import org.example.model.enums.UserRole;
import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.server.repository.UserDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Scanner;


class UserDaoTest {

    // Cấu hình URL kết nối H2 chạy trên bộ nhớ RAM giả lập MySQL
    // Thêm cụm từ đặc biệt ngẫu nhiên này vào URL của H2
    private static final String H2_URL = "jdbc:h2:mem:bidding_test_db;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String H2_USER = "sa";
    private static final String H2_PASSWORD = "";

    private Connection connection;
    private UserDao userDao;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        userDao = new UserDao();
        connection = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);

        // Đọc và thực thi file schema.sql theo từng câu lệnh đơn lẻ cách nhau bởi dấu ";"
        try (Statement stmt = connection.createStatement();
             InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("Không tìm thấy file schema.sql trong folder test resources!");
            }

            try (Scanner scanner = new Scanner(is).useDelimiter(";")) {
                while (scanner.hasNext()) {
                    String sqlLine = scanner.next().trim();
                    if (!sqlLine.isEmpty()) {
                        stmt.execute(sqlLine);
                    }
                }
            }
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            // Dọn dẹp sạch dữ liệu trong bảng users sau mỗi test case để tránh lỗi trùng lặp dữ liệu (PK constraint)
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET REFERENTIAL_INTEGRITY FALSE"); // Tắt kiểm tra khóa ngoại tạm thời để truncate
                stmt.execute("TRUNCATE TABLE users");
                stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
            connection.close();
        }
    }

    @Test
    void testCreateUser_Success() throws SQLException {
        // Arrange - Lưu ý: schema gốc của bạn yêu cầu fullname và email KHÔNG ĐƯỢC NULL
        Member newMember = new Member("minhanh2026", "hashed_password", "minhanh@gmail.com", "avatar.png", 1, 500000L, 0L);
        newMember.setFullname("Minh Anh");

        // Act
        boolean isCreated = userDao.createUser(connection, newMember);

        // Assert
        Assertions.assertTrue(isCreated);

        User fetchedUser = userDao.findByAccountname(connection, "minhanh2026");
        Assertions.assertNotNull(fetchedUser);
        Assertions.assertEquals("minhanh2026", fetchedUser.getAccountname());
        Assertions.assertEquals("Minh Anh", fetchedUser.getFullname());
        Assertions.assertEquals("minhanh@gmail.com", fetchedUser.getEmail());
        Assertions.assertEquals(UserRole.MEMBER, fetchedUser.getRole());

        if (fetchedUser instanceof Member member) {
            Assertions.assertEquals(500000L, member.getBalance());
        }
    }

    @Test
    void testFindByAccountname_NotFound() throws SQLException {
        // Act
        User user = userDao.findByAccountname(connection, "non_existent_user");

        // Assert
        Assertions.assertNull(user);
    }

    @Test
    void testAddBalance_AtomicIncrementAndDecrement() throws SQLException {
        // Arrange
        Member member = new Member("test_wallet", "pass", "wallet@test.com", "avt.jpg", 1, 1000L, 0L);
        member.setFullname("Wallet Test");
        userDao.createUser(connection, member);

        // Act: Cộng tiền (+500)
        boolean isAdded = userDao.addBalance(connection, "test_wallet", 500L);
        // Act: Trừ tiền (-200)
        boolean isSubtracted = userDao.addBalance(connection, "test_wallet", -200L);

        // Assert
        Assertions.assertTrue(isAdded);
        Assertions.assertTrue(isSubtracted);

        Member updatedMember = (Member) userDao.findByAccountname(connection, "test_wallet");
        Assertions.assertEquals(1300L, updatedMember.getBalance()); // 1000 + 500 - 200
    }

    @Test
    void testAddBlockedBalance_Success() throws SQLException {
        // Arrange - Số dư ban đầu phải thỏa mãn CHECK (balance >= blocked_balance) từ schema gốc của bạn
        Member member = new Member("bidder01", "pass", "bidder@test.com", "avt.jpg", 1, 2000L, 100L);
        member.setFullname("Bidder One");
        userDao.createUser(connection, member);

        // Act: Đóng băng thêm 400L tiền đấu giá (Tổng là 500L, vẫn bé hơn balance 2000L)
        boolean result = userDao.addBlockedBalance(connection, "bidder01", 400L);

        // Assert
        Assertions.assertTrue(result);
        Member updatedMember = (Member) userDao.findByAccountname(connection, "bidder01");
        Assertions.assertEquals(500L, updatedMember.getBlockedBalance());
    }

    @Test
    void testUpdateUserStatus_ActiveToBanned() throws SQLException {
        // Arrange
        Member member = new Member("bad_user", "pass", "bad@test.com", "avt.jpg", 1, 0, 0);
        member.setFullname("Bad User");
        userDao.createUser(connection, member);

        // Act: Đổi status sang 0 (Block/Banned)
        boolean isUpdated = userDao.updateUserStatus(connection, "bad_user", 0);

        // Assert
        Assertions.assertTrue(isUpdated);
        User updatedUser = userDao.findByAccountname(connection, "bad_user");
        Assertions.assertEquals(0, updatedUser.getStatus());
    }

    @Test
    void testGetUsersPaged_VerifyPaginationLogic() throws SQLException {
        // Arrange: Chèn 3 user mẫu vào database test
        Member uA = new Member("userA", "p", "a@t.com", "v", 1, 0, 0); uA.setFullname("A");
        Member uB = new Member("userB", "p", "b@t.com", "v", 1, 0, 0); uB.setFullname("B");
        Member uC = new Member("userC", "p", "c@t.com", "v", 1, 0, 0); uC.setFullname("C");

        userDao.createUser(connection, uA);
        userDao.createUser(connection, uB);
        userDao.createUser(connection, uC);

        // Act & Assert 1: Lấy trang đầu tiên, giới hạn tối đa 2 bản ghi (Sắp xếp theo accountname DESC)
        List<User> firstPage = userDao.getUsersPaged(connection, 2, 0);
        Assertions.assertEquals(2, firstPage.size());
        Assertions.assertEquals("userC", firstPage.get(0).getAccountname());
        Assertions.assertEquals("userB", firstPage.get(1).getAccountname());

        // Act & Assert 2: Lấy trang tiếp theo (Offset bỏ qua 2 bản ghi đầu)
        List<User> secondPage = userDao.getUsersPaged(connection, 2, 2);
        Assertions.assertEquals(1, secondPage.size());
        Assertions.assertEquals("userA", secondPage.get(0).getAccountname());
    }

    @Test
    void testGetTotalUsersCount_ReturnsCorrectCount() throws SQLException {
        // Arrange
        Member u1 = new Member("u1", "p", "1@t.com", "v", 1, 0, 0); u1.setFullname("One");
        Member u2 = new Member("u2", "p", "2@t.com", "v", 1, 0, 0); u2.setFullname("Two");

        userDao.createUser(connection, u1);
        userDao.createUser(connection, u2);

        // Act
        long totalCount = userDao.getTotalUsersCount(connection);

        // Assert
        Assertions.assertEquals(2, totalCount);
    }
}