# Database Schema & Data Integrity Constraints

This document details the Entity-Relationship Diagram (ERD) and the strict data integrity rules implemented at the database tier of the Bidding System. The database serves as the ultimate source of truth, enforcing critical business rules at the lowest level.

## 1. Entity-Relationship Diagram (ERD)

The following diagram visualizes the relational structure of the system's database.

```mermaid
erDiagram
    USERS ||--o{ PRODUCTS : "owns"
    USERS ||--o{ AUCTIONS : "sells"
    USERS ||--o{ AUCTIONS : "wins"
    USERS ||--o{ BIDS : "places"
    USERS ||--o{ AUTO_BIDS : "configures"
    USERS ||--o{ TRANSACTIONS : "sends/receives"
    
    PRODUCTS ||--o{ AUCTIONS : "is listed in"
    PRODUCTS ||--o{ TRANSACTIONS : "related to"
    
    AUCTIONS ||--o{ BIDS : "contains"
    AUCTIONS ||--o{ AUTO_BIDS : "has active"

    USERS {
        varchar(255) accountname PK "Primary Key, Login ID"
        varchar(255) fullname
        varchar(255) password "Hashed"
        varchar(255) email UK "Unique Identifier"
        bigint balance "Available funds"
        bigint blocked_balance "Funds held in escrow (bids)"
        int status "Active/Banned"
        int role "Admin/Member"
    }

    PRODUCTS {
        int product_id PK "Auto-increment"
        varchar(255) name
        int category "Enum mapping"
        varchar(255) owner_accountname FK "Current owner"
        boolean is_in_auction "Flag for active listings"
    }

    AUCTIONS {
        int auction_id PK "Auto-increment"
        int product_id FK "Item being auctioned"
        varchar(255) seller_accountname FK "Original seller"
        varchar(255) winner_accountname FK "Current highest bidder"
        bigint start_price
        bigint current_price "Current highest bid amount"
        bigint step_price "Minimum increment"
        bigint buy_now_price "Optional immediate buyout price"
        timestamp start_time
        timestamp end_time
        int status "Pending, Active, Closed"
        int version "Optimistic Locking version"
    }

    BIDS {
        int bid_id PK "Auto-increment"
        int auction_id FK
        varchar(255) bidder_accountname FK
        bigint bid_amount
        timestamp bid_time
    }

    AUTO_BIDS {
        int auto_bid_id PK "Auto-increment"
        int auction_id FK
        varchar(255) bidder_accountname FK
        bigint max_bid "Maximum willing to pay"
        bigint increment_amount "Step amount per auto-bid"
        boolean active "Is it currently running"
    }

    TRANSACTIONS {
        int transaction_id PK "Auto-increment"
        varchar(255) sender_accountname FK "Nullable"
        varchar(255) receiver_accountname FK "Nullable"
        int type "Deposit, Withdraw, Transfer, Hold"
        bigint amount
        int auction_id FK "Contextual reference"
        timestamp created_at
    }
```

## 2. Advanced Data Constraints (The Last Line of Defense)

To ensure the system is resilient against application-level bugs and race conditions, critical financial and business logic constraints are enforced directly in the MySQL database using `CHECK` constraints.

### 2.1 User Financial Protection (`users` table)
*   **Non-Negative Balance**: `CHECK (balance >= 0)` ensures an account can never be overdrawn.
*   **Non-Negative Escrow**: `CHECK (blocked_balance >= 0)` ensures escrow funds are logically sound.
*   **Total Asset Integrity**: `CHECK (balance >= blocked_balance)` guarantees that the system cannot freeze more funds than the user actually possesses.

### 2.2 Auction Integrity (`auctions` table)
*   **Positive Pricing**: `CHECK (start_price >= 0)` and `CHECK (step_price > 0)` enforce valid monetary values.
*   **Bid Escalation Rule**: `CHECK (current_price >= start_price)` prevents scenarios where the current price drops below the starting baseline.
*   **Buyout Logic**: `CHECK (buy_now_price IS NULL OR buy_now_price > start_price)` mandates that a buyout price must be a premium over the starting price.
*   **Chronological Order**: `CHECK (end_time > start_time)` prevents impossible auction durations.

### 2.3 Transaction Validity (`transactions` table)
*   **Meaningful Transfers**: `CHECK (amount > 0)` prevents zero-value transactions, mitigating potential database spam or denial-of-service vectors.

## 3. Foreign Key & Cascade Policies

The database utilizes strict foreign key referencing to maintain referential integrity, with carefully considered cascading policies:
*   **`ON DELETE CASCADE`**: When a `User` is deleted (if applicable), their `Products`, `Bids`, and `AutoBids` are automatically removed to maintain a clean state.
*   **`ON DELETE SET NULL`**: Financial history is immutable. When a `User` is removed, related `Transactions` are kept for auditing purposes. The `sender_accountname` or `receiver_accountname` fields are set to `NULL` to reflect the account's deletion while preserving the ledger.
