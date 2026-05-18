-- 1. Bảng người dùng (Users)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phonenumber VARCHAR(20),
    gender INT DEFAULT 0, -- 0: MALE, 1: FEMALE, 2: OTHER
    avt VARCHAR(1024),
    balance BIGINT DEFAULT 0, -- Tổng số tiền người dùng nạp vào
    blocked_balance BIGINT DEFAULT 0, -- Số tiền đang bị khóa do đang đặt giá cao nhất
    role INT NOT NULL, -- 0: ADMIN, 1: MEMBER
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng sản phẩm (Đóng vai trò là Phiên đấu giá)
CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    starting_price BIGINT NOT NULL,
    current_price BIGINT NOT NULL,
    step_price BIGINT NOT NULL,
    seller_id INT NOT NULL,
    winner_id INT DEFAULT NULL,
    category INT NOT NULL, -- 1: ELECTRONICS, 2: ART, 3: VEHICLE
    status INT DEFAULT 0,   -- 0: OPEN, 1: RUNNING, 2: FINISHED, 3: PAID, 4: CANCELED
    
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    
    version INT DEFAULT 0, 

    -- Các trường mở rộng
    brand VARCHAR(255),
    warranty_months INTEGER,
    artist VARCHAR(255),
    art_type VARCHAR(255),
    model VARCHAR(255),
    manufacture_year INTEGER,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- 3. Bảng lịch sử đấu giá
CREATE TABLE IF NOT EXISTS auctions (
    auction_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    bidder_id INT NOT NULL,
    bid_amount BIGINT NOT NULL,
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 4. Bảng cấu hình tự động đấu giá
CREATE TABLE IF NOT EXISTS auto_bids (
    auto_bid_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    user_id INT NOT NULL,
    max_bid_amount BIGINT NOT NULL,
    bid_increment BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_product (user_id, product_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
