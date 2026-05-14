-- 1. Xóa dữ liệu cũ nếu có
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE auctions;
TRUNCATE TABLE products;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- 2. Chèn 10 Người dùng mẫu
-- Mật khẩu cho tất cả: password123 (Đã băm BCrypt với Work Factor 12)
INSERT INTO users (user_id, username, password, email, phonenumber, gender, role, balance) VALUES
('u1', 'admin_minh', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'admin@bidding.vn', '0912345678', 'MALE', 'ADMIN', 0.00),
('u2', 'seller_nam', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'nam.seller@gmail.com', '0988887777', 'MALE', 'SELLER', 500.00),
('u3', 'apple_store', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'contact@apple.com', '0123456789', 'OTHER', 'SELLER', 10000.00),
('u4', 'bidder_huong', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'huong.bui@yahoo.com', '0977665544', 'FEMALE', 'BIDDER', 2000.00),
('u5', 'bidder_tuan', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'tuan.tran@gmail.com', '0900112233', 'MALE', 'BIDDER', 150.50),
('u6', 'gallery_art', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'art@gallery.vn', '0888999000', 'FEMALE', 'SELLER', 0.00),
('u7', 'bidder_long', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'long.nguyen@outlook.com', '0933445566', 'MALE', 'BIDDER', 5000.00),
('u8', 'bidder_an', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'an.thanh@gmail.com', '0944556677', 'FEMALE', 'BIDDER', 300.00),
('u9', 'auto_world', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'sales@autoworld.com', '0243123456', 'OTHER', 'SELLER', 50000.00),
('u10', 'bidder_linh', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'linh.kieu@gmail.com', '0911223344', 'FEMALE', 'BIDDER', 1200.00);

-- 3. Chèn Sản phẩm mẫu
INSERT INTO products (product_id, product_name, description, starting_price, step_price, seller_id, category, status, brand, model) VALUES
('p1', 'iPhone 15 Pro Max', 'Hàng mới 99%', 25000000, 500000, 'u3', 'ELECTRONICS', 'ACTIVE', 'Apple', '15 Pro Max'),
('p2', 'Tranh Sơn Dầu Phố Cổ', 'Tác phẩm của họa sĩ Bùi Xuân Phái', 50000000, 2000000, 'u6', 'ART', 'ACTIVE', NULL, NULL),
('p3', 'Tesla Model 3 2023', 'Xe điện nhập khẩu Mỹ', 1500000000, 10000000, 'u9', 'VEHICLE', 'ACTIVE', 'Tesla', 'Model 3');
