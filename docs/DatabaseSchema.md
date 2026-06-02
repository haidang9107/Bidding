# Database Schema & Data Integrity Constraints

This document details the Entity-Relationship Diagram (ERD) and the strict data integrity rules implemented at the database tier of the Bidding System. The database serves as the ultimate source of truth, enforcing critical business rules at the lowest level.

## 1. Entity-Relationship Diagram (ERD)

```mermaid
erDiagram
    USERS ||--o{ PRODUCTS : "owns"
    USERS ||--o{ AUCTIONS : "sells"
    USERS ||--o{ AUCTIONS : "wins (nullable)"
    USERS ||--o{ BIDS : "places"
    USERS ||--o{ AUTO_BIDS : "configures"
    USERS ||--o{ TRANSACTIONS : "sends / receives (nullable)"

    PRODUCTS ||--o{ AUCTIONS : "listed in (1 to many)"
    PRODUCTS ||--o{ TRANSACTIONS : "referenced by"

    AUCTIONS ||--o{ BIDS : "contains"
    AUCTIONS ||--o{ AUTO_BIDS : "has active"
    AUCTIONS ||--o{ TRANSACTIONS : "referenced by"

    USERS {
        varchar accountname PK "Login ID"
        varchar fullname
        varchar password "Bcrypt hashed"
        varchar email UK "Unique"
        varchar avt "Avatar URL (nullable)"
        bigint balance "Total funds CHECK >= 0"
        bigint blocked_balance "Escrow CHECK >= 0"
        int role "0=MEMBER, 1=ADMIN"
        int status "0=ACTIVE, 1=BANNED"
    }

    PRODUCTS {
        int product_id PK "Auto-increment"
        varchar name
        text description
        varchar image_url "Nullable"
        int category "Enum: 0=ELECTRONICS 1=ART 2=VEHICLE 3=OTHER"
        varchar owner_accountname FK "Current owner"
        boolean is_in_auction "Denorm flag for fast listing queries"
        timestamp withdrawn_at "Nullable — set when product is removed"
        varchar brand "Electronics / Vehicle only"
        int warranty_months "Electronics only"
        varchar artist "Art only"
        varchar art_type "Art only"
        varchar model "Vehicle only"
        int manufacture_year "Vehicle only"
        timestamp created_at
    }

    AUCTIONS {
        int auction_id PK "Auto-increment"
        int product_id FK "Product being auctioned"
        varchar seller_accountname FK "Original seller"
        varchar winner_accountname FK "Current leading bidder (nullable)"
        bigint start_price "CHECK >= 0"
        bigint step_price "Minimum increment CHECK > 0"
        bigint current_price "CHECK >= start_price"
        bigint buy_now_price "Nullable — immediate buyout price CHECK > 0"
        timestamp start_time
        timestamp end_time "CHECK end_time > start_time"
        int status "0=OPEN 1=RUNNING 2=FINISHED 3=CANCELED"
        int version "Optimistic lock counter"
        timestamp created_at
    }

    BIDS {
        int bid_id PK "Auto-increment"
        int auction_id FK
        varchar bidder_accountname FK
        bigint bid_amount "CHECK > 0"
        timestamp bid_time
        boolean is_auto_bid "True if placed by auto-bidder"
    }

    AUTO_BIDS {
        int auto_bid_id PK "Auto-increment"
        int auction_id FK
        varchar bidder_accountname FK
        bigint max_bid "Maximum willing to pay CHECK > 0"
        bigint increment_amount "Step per auto-bid CHECK > 0"
        boolean active "Whether this config is currently live"
        timestamp created_at
        timestamp updated_at
    }

    TRANSACTIONS {
        int transaction_id PK "Auto-increment"
        varchar sender_accountname FK "Nullable (e.g. system deposit)"
        varchar receiver_accountname FK "Nullable (e.g. withdrawal)"
        int type "0=DEPOSIT 1=WITHDRAW 2=TRANSFER 3=AUCTION_PAYMENT"
        integer product_id FK "Nullable — asset context"
        bigint amount "CHECK >= 0"
        integer auction_id FK "Nullable — auction context"
        text description
        timestamp created_at
    }

    WATCHLIST {
        varchar user_accountname PK, FK
        int auction_id PK, FK
        timestamp created_at
    }
```

## 2. Data Integrity Constraints

Critical financial and business rules are enforced directly in MySQL using `CHECK` constraints and foreign keys, acting as a safety net against application-level bugs.

### 2.1 User Financial Protection (`users`)
| Constraint | Rule |
|---|---|
| `CHECK (balance >= 0)` | Account can never be overdrawn |
| `CHECK (blocked_balance >= 0)` | Escrow funds are always non-negative |
| `CHECK (balance >= blocked_balance)` | System cannot freeze more than the user owns |

### 2.2 Auction Integrity (`auctions`)
| Constraint | Rule |
|---|---|
| `CHECK (start_price >= 0)` | Valid monetary starting value |
| `CHECK (step_price > 0)` | Bid increment must be positive |
| `CHECK (current_price >= start_price)` | Price can never drop below starting |
| `CHECK (buy_now_price IS NULL OR buy_now_price > 0)` | Buy-now must be positive if set |
| `CHECK (end_time > start_time)` | Auction must have a positive duration |

> **Note**: The application additionally validates that `buy_now_price > start_price` at the service layer before persisting.

### 2.3 Transaction Validity (`transactions`)
| Constraint | Rule |
|---|---|
| `CHECK (amount >= 0)` | No negative-value transactions |

### 2.4 Bid Validity (`bids`)
| Constraint | Rule |
|---|---|
| `CHECK (bid_amount > 0)` | Every bid must be a positive amount |

## 3. Data Access Objects (DAO)
All persistence logic is encapsulated in Singleton DAOs (e.g., `AuctionDao.getInstance()`), ensuring thread-safe access to the database via the `TransactionManager`.

## 4. Foreign Key Cascade Policies

| Relationship | On Delete |
|---|---|
| `products.owner_accountname → users` | `CASCADE` — deletes user's products |
| `auctions.product_id → products` | `CASCADE` — removes auctions for deleted products |
| `auctions.seller_accountname → users` | `CASCADE` |
| `auctions.winner_accountname → users` | `SET NULL` — preserves auction record |
| `bids.auction_id → auctions` | `CASCADE` |
| `bids.bidder_accountname → users` | `CASCADE` |
| `auto_bids.auction_id → auctions` | `CASCADE` |
| `auto_bids.bidder_accountname → users` | `CASCADE` |
| `transactions.sender_accountname → users` | `SET NULL` — financial history is immutable |
| `transactions.receiver_accountname → users` | `SET NULL` |
| `transactions.product_id → products` | `SET NULL` |
| `transactions.auction_id → auctions` | `SET NULL` |
| `watchlist.user_accountname → users` | `CASCADE` |
| `watchlist.product_id → products` | `CASCADE` |

## 4. Indexes

| Index | Columns | Purpose |
|---|---|---|
| `idx_products_owner` | `owner_accountname` | Fast "my inventory" queries |
| `idx_products_auction` | `is_in_auction` | Filter active listings |
| `idx_auctions_product` | `product_id` | Auction history per product |
| `idx_auctions_status_end_time` | `(status, end_time)` | AuctionMonitor expired/upcoming lookups |
| `idx_bids_auction_amount` | `(auction_id, bid_amount DESC)` | Auto-bidding sort |
| `idx_auto_bids_auction` | `(auction_id, active, max_bid)` | Active auto-bid lookups |
| `idx_watchlist_user` | `user_accountname` | User's watchlist lookup |
| `idx_watchlist_product` | `product_id` | Find all watchers of a product |
| `idx_transactions_sender` | `sender_accountname` | Transaction history |
| `idx_transactions_receiver` | `receiver_accountname` | Transaction history |
| `idx_transactions_auction` | `auction_id` | Auction payment lookups |