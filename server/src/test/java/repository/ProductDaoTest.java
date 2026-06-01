package repository;

import org.example.model.product.*;
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
import java.util.List;
import java.util.Scanner; // Thêm import này để hết lỗi đỏ Scanner

import static org.junit.jupiter.api.Assertions.*;

class ProductDaoTest {

    private static final String H2_URL = "jdbc:h2:mem:bidding_test_db;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private Connection connection;
    private ProductDao productDao;
    private Object Category;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection(H2_URL, USER, PASSWORD);

        // Nạp cấu trúc schema sạch vào RAM trước mỗi bài test
        importSchema();

        productDao = ProductDao.getInstance();

        // BẮT BUỘC: Tạo sẵn user để thỏa mãn Khóa ngoại (Foreign Key) cho bảng products
        insertMockUser("minhanh", "Minh Anh", "pass123", "minhanh@gmail.com");
        insertMockUser("seller2", "Nguyen Van B", "pass456", "seller2@gmail.com");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // ====================================================================
    // 1. TEST HÀM INSERT PRODUCT (THỬ NGHIỆM ĐA HÌNH VỚI ELECTRONICS)
    // ====================================================================
    @Test
    void testInsertProduct_Success() throws SQLException {
        Electronics laptop = new Electronics();
        laptop.setName("ThinkPad X1 Carbon");
        laptop.setDescription("Máy đẹp keng");
        laptop.setImageUrl("http://example.com/laptop.png");
        // BỎ DÒNG SETCATEGORY Ở ĐÂY vì class Electronics khi khởi tạo đã tự mang định danh của nó
        laptop.setOwnerAccountname("minhanh");
        laptop.setInAuction(false);
        laptop.setBrand("Lenovo");
        laptop.setWarrantyMonths(24);

        boolean result = productDao.insertProduct(connection, laptop);

        assertTrue(result);
        assertTrue(laptop.getProductId() > 0, "ID sản phẩm phải tự tăng lớn hơn 0 sau khi insert thành công");
    }

    // ====================================================================
    // 2. TEST HÀM LẤY PRODUCT THEO ID
    // ====================================================================
    @Test
    void testGetProductById_Success() throws SQLException {
        Art painting = new Art();
        painting.setName("Mona Lisa Replica");
        painting.setDescription("Tranh sơn dầu");
        painting.setImageUrl("http://example.com/art.png");
        // BỎ DÒNG SETCATEGORY Ở ĐÂY
        painting.setOwnerAccountname("minhanh");
        painting.setInAuction(false);
        painting.setArtist("Da Vinci");
        painting.setArtType("Oil Painting");

        productDao.insertProduct(connection, painting);

        Product dbProduct = productDao.getProductById(connection, painting.getProductId());

        assertNotNull(dbProduct);
        assertEquals("Mona Lisa Replica", dbProduct.getName());
        assertEquals("minhanh", dbProduct.getOwnerAccountname());
    }

    @Test
    void testGetProductById_NotFound() throws SQLException {
        Product p = productDao.getProductById(connection, 9999);
        assertNull(p, "Nếu ID không tồn tại trong DB, hàm phải trả về null");
    }

    // ====================================================================
    // 3. TEST HÀM LẤY PRODUCT KÈM THEO ROW LOCK (FOR UPDATE)
    // ====================================================================
    @Test
    void testGetProductForUpdate_Success() throws SQLException {
        Vehicle car = new Vehicle();
        car.setName("VinFast VF8");
        // BỎ DÒNG SETCATEGORY Ở ĐÂY
        car.setOwnerAccountname("minhanh");
        car.setBrand("VinFast");
        car.setModel("VF8 Plus");
        car.setManufactureYear(2024);

        productDao.insertProduct(connection, car);

        Product dbProduct = productDao.getProductForUpdate(connection, car.getProductId());
        assertNotNull(dbProduct);
        assertEquals("VinFast VF8", dbProduct.getName());
    }

    // ====================================================================
    // 4. TEST HÀM LẤY DANH SÁCH SẢN PHẨM CỦA MỘT CHỦ SỞ HỮU
    // ====================================================================
    @Test
    void testGetProductsByOwner_Success() throws SQLException {
        Electronics item1 = new Electronics();
        item1.setName("iPhone 15");
        // BỎ DÒNG SETCATEGORY Ở ĐÂY
        item1.setOwnerAccountname("minhanh");
        productDao.insertProduct(connection, item1);

        Electronics item2 = new Electronics();
        item2.setName("iPad Pro");
        // BỎ DÒNG SETCATEGORY Ở ĐÂY
        item2.setOwnerAccountname("minhanh");
        productDao.insertProduct(connection, item2);

        List<Product> products = productDao.getProductsByOwner(connection, "minhanh");

        assertEquals(2, products.size(), "User 'minhanh' phải có đúng 2 sản phẩm");
        assertEquals("iPad Pro", products.get(0).getName());
    }

    // ====================================================================
    // 5. TEST HÀM CẬP NHẬT TRẠNG THÁI ĐANG ĐẤU GIÁ (AUCTION FLAG)
    // ====================================================================
    @Test
    void testUpdateProductAuctionFlag_Success() throws SQLException {
        Electronics tv = new Electronics();
        tv.setName("Sony TV 4K");
        // BỎ DÒNG SETCATEGORY Ở ĐÂY
        tv.setOwnerAccountname("minhanh");
        tv.setInAuction(false);
        productDao.insertProduct(connection, tv);

        boolean updated = productDao.updateProductAuctionFlag(connection, tv.getProductId(), true);

        assertTrue(updated);
        Product dbProduct = productDao.getProductById(connection, tv.getProductId());
        assertTrue(dbProduct.isInAuction(), "Flag is_in_auction phải chuyển sang TRUE");
    }

    // ====================================================================
    // 6. TEST HÀM CHUYỂN QUYỀN SỞ HỮU SẢN PHẨM KHI ĐẤU GIÁ THÀNH CÔNG
    // ====================================================================
    @Test
    void testUpdateProductOwner_Success() throws SQLException {
        Electronics watch = new Electronics();
        watch.setName("Apple Watch Ultra");
        // BỎ DÒNG SETCATEGORY Ở ĐÂY
        watch.setOwnerAccountname("minhanh");
        watch.setInAuction(true);
        productDao.insertProduct(connection, watch);

        boolean updated = productDao.updateProductOwner(connection, watch.getProductId(), "seller2");

        assertTrue(updated);
        Product dbProduct = productDao.getProductById(connection, watch.getProductId());
        assertEquals("seller2", dbProduct.getOwnerAccountname(), "Chủ mới phải là seller2");
        assertFalse(dbProduct.isInAuction(), "Sau khi bán, cờ is_in_auction phải tự động về FALSE");
    }
    // ====================================================================
    // HÀM HỖ TRỢ ĐỌC SCHEMA VÀ CHÈN MOCK DATA
    // ====================================================================
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