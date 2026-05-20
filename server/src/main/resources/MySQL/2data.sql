-- 1. Xóa dữ liệu cũ
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE bids;
TRUNCATE TABLE products;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- 2. Chèn Người dùng mẫu (Cấu trúc mới: accountname, password, email, avt, balance, blocked_balance, role, status)
-- Password là 'password123' đã được hash BCrypt
INSERT INTO users (accountname, password, email, avt, balance, blocked_balance, role, status) VALUES
('admin_minh', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'admin@bidding.vn', NULL, 0, 0, 0, 0),
('nam_member', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'nam.member@gmail.com', NULL, 50000000, 0, 1, 0),
('apple_store', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'contact@apple.com', NULL, 100000000, 0, 1, 0),
('huong_member', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'huong.bui@yahoo.com', NULL, 20000000, 0, 1, 0);

-- 3. Chèn Sản phẩm mẫu (Cấu trúc mới dùng accountname cho seller và winner)
INSERT INTO products (name, description, image_url, category, seller_accountname, start_price, step_price, current_price, status, end_time) VALUES
('iPhone 15 Pro Max', 'Hàng mới 99%', 'https://example.com/iphone15.jpg', 0, 'apple_store', 25000000, 500000, 25000000, 1, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY)),
('Tesla Model 3', 'Xe điện Mỹ', 'https://example.com/tesla.jpg', 1, 'nam_member', 1500000000, 10000000, 1500000000, 1, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 10 DAY));
