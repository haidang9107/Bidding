-- PostgreSQL Schema for Bidding System

-- Drop tables if they exist (for clean setup)
DROP TABLE IF EXISTS auto_bids;
DROP TABLE IF EXISTS bids;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS auctions;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;

-- 1. Users table
CREATE TABLE users (
    accountname VARCHAR(255) PRIMARY KEY,
    fullname VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    avt VARCHAR(1024),
    balance BIGINT DEFAULT 0 CHECK (balance >= 0),
    blocked_balance BIGINT DEFAULT 0 CHECK (blocked_balance >= 0),
    role INT NOT NULL, -- 0: ADMIN, 1: MEMBER
    status INT DEFAULT 0 -- 0: ACTIVE, 1: BANNED
);

-- 2. Products table (Removed is_in_auction, added withdrawn_at)
CREATE TABLE products (
    product_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(1024),
    category INT NOT NULL, -- 0: ELECTRONICS, 1: ART, 2: VEHICLE, 3: OTHER
    owner_accountname VARCHAR(255) NOT NULL REFERENCES users(accountname) ON DELETE CASCADE,
    withdrawn_at TIMESTAMP, -- Date product was withdrawn from the system
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Category-specific fields
    brand VARCHAR(255),
    warranty_months INT,
    artist VARCHAR(255),
    art_type VARCHAR(1024),
    model VARCHAR(255),
    manufacture_year INT
);

-- 3. Auctions table (Session management)
CREATE TABLE auctions (
    auction_id SERIAL PRIMARY KEY,
    product_id INT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    seller_accountname VARCHAR(255) NOT NULL REFERENCES users(accountname) ON DELETE CASCADE,
    winner_accountname VARCHAR(255) REFERENCES users(accountname) ON DELETE SET NULL,
    start_price BIGINT NOT NULL CHECK (start_price >= 0),
    step_price BIGINT NOT NULL CHECK (step_price > 0),
    current_price BIGINT NOT NULL CHECK (current_price >= 0),
    buy_now_price BIGINT CHECK (buy_now_price IS NULL OR buy_now_price > 0),
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NOT NULL,
    status INT DEFAULT 0, -- 0: OPEN, 1: RUNNING, 2: FINISHED, 3: CANCELED
    version INT DEFAULT 0, -- For optimistic locking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bids table (Individual bid history)
CREATE TABLE bids (
    bid_id SERIAL PRIMARY KEY,
    auction_id INT NOT NULL REFERENCES auctions(auction_id) ON DELETE CASCADE,
    bidder_accountname VARCHAR(255) NOT NULL REFERENCES users(accountname) ON DELETE CASCADE,
    bid_amount BIGINT NOT NULL CHECK (bid_amount > 0),
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_auto_bid BOOLEAN DEFAULT FALSE
);

-- 5. Auto Bids table (Config for automated bidding)
CREATE TABLE auto_bids (
    auto_bid_id SERIAL PRIMARY KEY,
    auction_id INT NOT NULL REFERENCES auctions(auction_id) ON DELETE CASCADE,
    bidder_accountname VARCHAR(255) NOT NULL REFERENCES users(accountname) ON DELETE CASCADE,
    max_bid BIGINT NOT NULL CHECK (max_bid > 0),
    increment_amount BIGINT NOT NULL CHECK (increment_amount > 0),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (auction_id, bidder_accountname)
);

-- 6. Transactions table (Financial history)
CREATE TABLE transactions (
    transaction_id SERIAL PRIMARY KEY,
    sender_accountname VARCHAR(255) REFERENCES users(accountname) ON DELETE SET NULL,
    receiver_accountname VARCHAR(255) REFERENCES users(accountname) ON DELETE SET NULL,
    type INT NOT NULL, -- Transaction type enum
    product_id INT REFERENCES products(product_id) ON DELETE SET NULL,
    auction_id INT REFERENCES auctions(auction_id) ON DELETE SET NULL,
    amount BIGINT DEFAULT 0 CHECK (amount >= 0),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_products_owner ON products(owner_accountname);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_auctions_product ON auctions(product_id);
CREATE INDEX idx_auctions_status_end_time ON auctions(status, end_time);
CREATE INDEX idx_bids_auction_amount ON bids(auction_id, bid_amount DESC);
CREATE INDEX idx_auto_bids_auction_active ON auto_bids(auction_id, active);
CREATE INDEX idx_transactions_sender ON transactions(sender_accountname);
CREATE INDEX idx_transactions_receiver ON transactions(receiver_accountname);
CREATE INDEX idx_transactions_auction ON transactions(auction_id);
