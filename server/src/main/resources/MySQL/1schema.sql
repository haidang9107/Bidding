CREATE TABLE IF NOT EXISTS users (
    accountname VARCHAR(255) PRIMARY KEY,
    fullname VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    avt VARCHAR(1024) DEFAULT NULL,
    balance BIGINT DEFAULT 0 CHECK (balance >= 0),
    blocked_balance BIGINT DEFAULT 0 CHECK (blocked_balance >= 0),
    role INT NOT NULL,
    status INT DEFAULT 0,
    CHECK (balance >= blocked_balance)
);

CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(1024),
    category INT NOT NULL,
    owner_accountname VARCHAR(255) NOT NULL,
    is_in_auction BOOLEAN DEFAULT FALSE,
    withdrawn_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    brand VARCHAR(255),
    warranty_months INT,
    artist VARCHAR(255),
    art_type VARCHAR(1024),
    model VARCHAR(255),
    manufacture_year INT,
    FOREIGN KEY (owner_accountname) REFERENCES users(accountname) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auctions (
    auction_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    seller_accountname VARCHAR(255) NOT NULL,
    winner_accountname VARCHAR(255) DEFAULT NULL,
    start_price BIGINT NOT NULL CHECK (start_price >= 0),
    step_price BIGINT NOT NULL CHECK (step_price > 0),
    current_price BIGINT NOT NULL CHECK (current_price >= 0),
    buy_now_price BIGINT DEFAULT NULL CHECK (buy_now_price IS NULL OR buy_now_price > 0),
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NOT NULL,
    status INT DEFAULT 0,
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (seller_accountname) REFERENCES users(accountname) ON DELETE CASCADE,
    FOREIGN KEY (winner_accountname) REFERENCES users(accountname) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS bids (
    bid_id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    bidder_accountname VARCHAR(255) NOT NULL,
    bid_amount BIGINT NOT NULL CHECK (bid_amount > 0),
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_auto_bid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_accountname) REFERENCES users(accountname) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auto_bids (
    auto_bid_id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    bidder_accountname VARCHAR(255) NOT NULL,
    max_bid BIGINT NOT NULL CHECK (max_bid > 0),
    increment_amount BIGINT NOT NULL CHECK (increment_amount > 0),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_auto_bid_auction_bidder (auction_id, bidder_accountname),
    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_accountname) REFERENCES users(accountname) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    sender_accountname VARCHAR(255) NULL,
    receiver_accountname VARCHAR(255) NULL,
    type INT NOT NULL,
    product_id INT NULL,
    amount BIGINT NOT NULL CHECK (amount >= 0),
    auction_id INT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_accountname) REFERENCES users(accountname) ON DELETE SET NULL,
    FOREIGN KEY (receiver_accountname) REFERENCES users(accountname) ON DELETE SET NULL,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE SET NULL,
    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id) ON DELETE SET NULL
);

CREATE INDEX idx_products_owner ON products(owner_accountname);
CREATE INDEX idx_products_auction ON products(is_in_auction);
CREATE INDEX idx_auctions_product ON auctions(product_id);
CREATE INDEX idx_auctions_status_end_time ON auctions(status, end_time);
CREATE INDEX idx_bids_auction_amount ON bids(auction_id, bid_amount DESC);
CREATE INDEX idx_auto_bids_auction ON auto_bids(auction_id, active, max_bid);
CREATE INDEX idx_transactions_sender ON transactions(sender_accountname);
CREATE INDEX idx_transactions_receiver ON transactions(receiver_accountname);
CREATE INDEX idx_transactions_auction ON transactions(auction_id);
