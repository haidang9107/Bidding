-- 1. Xóa dữ liệu cũ
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE auto_bids;
TRUNCATE TABLE auctions;
TRUNCATE TABLE products;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- 2. Chèn Người dùng mẫu
-- balance: Tổng tiền, blocked_balance: Tiền tạm khóa
INSERT INTO users (username, password, email, phonenumber, gender, role, balance, blocked_balance) VALUES
('admin_minh', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'admin@bidding.vn', '0912345678', 0, 0, 0, 0),
('nam_member', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'nam.member@gmail.com', '0988887777', 0, 1, 50000000, 0),
('apple_store', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'contact@apple.com', '0123456789', 2, 1, 100000000, 0),
('huong_member', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'huong.bui@yahoo.com', '0977665544', 1, 1, 20000000, 0);

-- 3. Chèn Sản phẩm mẫu
INSERT INTO products (product_name, description, starting_price, current_price, step_price, seller_id, category, status, start_time, end_time, brand, model) VALUES
('iPhone 15 Pro Max', 'Hàng mới 99%', 25000000, 25000000, 500000, 3, 1, 1, 
 CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY), 'Apple', '15 Pro Max'),
('Tesla Model 3 2023', 'Xe điện nhập khẩu Mỹ', 1500000000, 1500000000, 10000000, 2, 3, 0, 
 DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 1 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 10 DAY), 'Tesla', 'Model 3');
