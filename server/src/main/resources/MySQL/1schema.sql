-- 1. Bảng người dùng (Users)
CREATE TABLE IF NOT EXISTS users (
    accountname VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    avt VARCHAR(1024) DEFAULT NULL,
    balance BIGINT DEFAULT 0, -- Tổng số tiền người dùng nạp vào
    blocked_balance BIGINT DEFAULT 0, -- Số tiền đang bị khóa do đang đặt giá cao nhất
    role INT NOT NULL, -- 0: ADMIN, 1: MEMBER
    status INT DEFAULT 0 -- 0: ACTIVE, 1: BANNED
);

-- 2. Bảng sản phẩm (Đóng vai trò là Phiên đấu giá)
CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(1024),
    category INT NOT NULL, -- 0: ELECTRONICS, 1: VEHICLE, 2: ART, 3: OTHER
    seller_accountname VARCHAR(255) NOT NULL,
    start_price BIGINT NOT NULL,
    step_price BIGINT NOT NULL,
    current_price BIGINT NOT NULL,
    winner_accountname VARCHAR(255) DEFAULT NULL,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NOT NULL,
    status INT DEFAULT 0, -- 0: PENDING, 1: ACTIVE, 2: FINISHED, 3: CANCELED
    version INT DEFAULT 0, -- Dùng cho Optimistic Locking
    
    -- Specific fields for categories
    brand VARCHAR(255),
    warranty_months INT,
    artist VARCHAR(255),
    art_type VARCHAR(1024),
    model VARCHAR(255),
    manufacture_year INT,

    FOREIGN KEY (seller_accountname) REFERENCES users(accountname) ON DELETE CASCADE,
    FOREIGN KEY (winner_accountname) REFERENCES users(accountname) ON DELETE SET NULL
);

-- 3. Bảng lịch sử đặt giá (Bids)
CREATE TABLE IF NOT EXISTS bids (
    bid_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    bidder_accountname VARCHAR(255) NOT NULL,
    bid_amount BIGINT NOT NULL,
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_accountname) REFERENCES users(accountname) ON DELETE CASCADE
);