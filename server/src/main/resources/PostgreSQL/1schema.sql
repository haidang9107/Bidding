-- Phân loại giới tính
CREATE TYPE gender_enum AS ENUM ('MALE', 'FEMALE', 'OTHER', 'UNKNOWN');

-- Phân loại vai trò người dùng
CREATE TYPE role_enum AS ENUM ('ADMIN', 'SELLER', 'BIDDER');

-- Phân loại trạng thái sản phẩm
CREATE TYPE product_status_enum AS ENUM ('ACTIVE', 'SOLD', 'CLOSED', 'PENDING');

-- Phân loại danh mục sản phẩm (Category)
CREATE TYPE category_enum AS ENUM ('ELECTRONICS', 'ART', 'VEHICLE');

-- 1. Bảng người dùng (Users)
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phonenumber VARCHAR(20),
    gender gender_enum DEFAULT 'UNKNOWN', -- Sử dụng ENUM
    avt TEXT,
    balance DECIMAL(15, 2) DEFAULT 0.00,
    role role_enum NOT NULL,              -- Sử dụng ENUM
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
    category category_enum NOT NULL,       -- Sử dụng ENUM
    status product_status_enum DEFAULT 'ACTIVE', -- Sử dụng ENUM

-- Các trường mở rộng
    brand VARCHAR(255),
    warranty_months INTEGER,
    artist VARCHAR(255),
    art_type VARCHAR(255),
    model VARCHAR(255),
    manufacture_year INTEGER,
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