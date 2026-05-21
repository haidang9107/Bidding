SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE transactions;
TRUNCATE TABLE auto_bids;
TRUNCATE TABLE bids;
TRUNCATE TABLE auctions;
TRUNCATE TABLE products;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users (accountname, fullname, password, email, avt, balance, blocked_balance, role, status) VALUES
('admin_minh', 'Minh Admin', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'admin@bidding.vn', NULL, 0, 0, 0, 0),
('nam_member', 'Nguyen Van Nam', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'nam.member@gmail.com', NULL, 50000000, 0, 1, 0),
('apple_store', 'Apple Store Vietnam', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'contact@apple.com', NULL, 100000000, 0, 1, 0),
('huong_member', 'Bui Thu Huong', '$2a$12$PRsYuZXgIuYP3SlwPdci8..uOTEVtI72.f.romyqYpk0BUDevkz52', 'huong.bui@yahoo.com', NULL, 20000000, 0, 1, 0);

INSERT INTO products (name, description, image_url, category, owner_accountname, is_in_auction, brand, warranty_months, model, manufacture_year) VALUES
('iPhone 15 Pro Max', 'Hang moi 99%', 'https://example.com/iphone15.jpg', 0, 'apple_store', TRUE, 'Apple', 12, NULL, NULL),
('Tesla Model 3', 'Xe dien My', 'https://example.com/tesla.jpg', 1, 'nam_member', TRUE, 'Tesla', NULL, 'Model 3', 2023);

INSERT INTO auctions (product_id, seller_accountname, start_price, step_price, current_price, buy_now_price, status, end_time) VALUES
(1, 'apple_store', 25000000, 500000, 25000000, 35000000, 1, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY)),
(2, 'nam_member', 1500000000, 10000000, 1500000000, NULL, 1, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 10 DAY));
