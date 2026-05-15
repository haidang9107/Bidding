CREATE TABLE users (
    user_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phonenumber VARCHAR(20),
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    avt TEXT,
    balance DECIMAL(15, 2) DEFAULT 0.00,
    role ENUM('ADMIN', 'USER', 'SELLER') DEFAULT 'USER',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE TABLE products (
    product_id VARCHAR(255) PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    starting_price DECIMAL(15, 2) NOT NULL,
    step_price DECIMAL(15, 2) NOT NULL,
    seller_id VARCHAR(255),
    category ENUM('ELECTRONICS', 'ART', 'FASHION', 'COLLECTIBLES'), -- Bạn có thể sửa các option này
    status ENUM('PENDING', 'ACTIVE', 'SOLD', 'CANCELLED') DEFAULT 'PENDING',
    brand VARCHAR(255),
    warranty_months INT,
    artist VARCHAR(255),
    art_type VARCHAR(255),
    model VARCHAR(255),
    manufacture_year INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id)
        REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE TABLE auctions (
    auction_id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255),
    bidder_id VARCHAR(255),
    bid_amount DECIMAL(15, 2) NOT NULL,
    bid_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_auction_product FOREIGN KEY (product_id)
        REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_auction_bidder FOREIGN KEY (bidder_id)
        REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;