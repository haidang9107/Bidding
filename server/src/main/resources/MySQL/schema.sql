-- 1. Bảng người dùng (Users)
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phonenumber VARCHAR(20),
    gender VARCHAR(20),
    avt TEXT,
    balance DECIMAL(15, 2) DEFAULT 0.00,
    role VARCHAR(50) NOT NULL, -- ADMIN, SELLER, BIDDER
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng sản phẩm (Products/Items)
CREATE TABLE IF NOT EXISTS products (
    product_id VARCHAR(255) PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    starting_price DECIMAL(15, 2) NOT NULL,
    step_price DECIMAL(15, 2) NOT NULL,
    seller_id VARCHAR(255) REFERENCES users(user_id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL, -- Electronics, Art, Vehicle
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, SOLD, CLOSED
    
    -- Các trường mở rộng cho từng loại sản phẩm (Inheritance Table-per-Hierarchy)
    brand VARCHAR(255),          -- Dùng cho Electronics, Vehicle
    warranty_months INTEGER,     -- Dùng cho Electronics
    artist VARCHAR(255),         -- Dùng cho Art
    art_type VARCHAR(255),       -- Dùng cho Art
    model VARCHAR(255),          -- Dùng cho Vehicle
    manufacture_year INTEGER,    -- Dùng cho Vehicle
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng lịch sử đấu giá (Auctions)
CREATE TABLE IF NOT EXISTS auctions (
    auction_id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255) REFERENCES products(product_id) ON DELETE CASCADE,
    bidder_id VARCHAR(255) REFERENCES users(user_id) ON DELETE CASCADE,
    bid_amount DECIMAL(15, 2) NOT NULL,
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
