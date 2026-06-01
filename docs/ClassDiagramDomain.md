# Core Domain Model Architecture

This document outlines the core domain model of the Bidding System, detailing the primary entities, their attributes, and the relationships that drive the business logic of the application.

## 1. Domain Entities Overview

The system is built around several key entities:
*   **User Hierarchy**: Represents the participants in the system, utilizing inheritance to distinguish between regular `Member`s and administrative `Admin`s.
*   **Product Hierarchy**: Represents the physical items that users own, with specific subtypes for various categories (`Electronics`, `Vehicle`, `Art`, `OtherItem`). A `Product` is independent of any auction — the same product can be listed for auction multiple times.
*   **Auction**: Represents a single auction session tied to a `Product`. Manages the entire lifecycle from `OPEN` → `RUNNING` → `FINISHED`/`CANCELED`.
*   **Bidding Mechanics**: `Bid` records each individual bid placed; `AutoBid` stores an automatic bidding configuration for a user on a given auction.
*   **Financials**: The `Transaction` entity records all financial movements, ensuring auditability and balance integrity.

## 2. Class Diagram

```mermaid
classDiagram
    %% User Hierarchy
    class User {
        <<abstract>>
        -String accountname
        -String fullname
        -String password
        -String email
        -String avt
        -UserRole role
        -int status
        +getters()
        +setters()
    }

    class Admin {
    }

    class Member {
        -long balance
        -long blockedBalance
        +getAvailableBalance() long
    }

    User <|-- Admin
    User <|-- Member

    %% Product Hierarchy
    class Product {
        <<abstract>>
        -int productId
        -String name
        -String description
        -String imageUrl
        -ItemCategory category
        -String ownerAccountname
        -boolean inAuction
        -Timestamp withdrawnAt
        +getters()
        +setters()
    }

    class Electronics {
        -String brand
        -Integer warrantyMonths
    }

    class Vehicle {
        -String brand
        -String model
        -Integer manufactureYear
    }

    class Art {
        -String artist
        -String artType
    }

    class OtherItem {
    }

    Product <|-- Electronics
    Product <|-- Vehicle
    Product <|-- Art
    Product <|-- OtherItem

    %% Auction Session
    class Auction {
        -int auctionId
        -int productId
        -String sellerAccountname
        -String winnerAccountname
        -long startingPrice
        -long currentPrice
        -long stepPrice
        -Long buyNowPrice
        -Timestamp startTime
        -Timestamp endTime
        -AuctionStatus status
        -int version
        -Product product
        +getters()
        +setters()
    }

    %% Bidding Mechanics
    class Bid {
        -int auctionId
        -String bidderAccountname
        -long bidAmount
        -Timestamp bidTime
    }

    class AutoBid {
        -int autoBidId
        -int auctionId
        -String bidderAccountname
        -long maxBid
        -long incrementAmount
        -boolean active
        -Timestamp createdAt
        -Timestamp updatedAt
    }

    class Transaction {
        -int transactionId
        -String senderAccountname
        -String receiverAccountname
        -TransactionType type
        -Integer productId
        -long amount
        -Integer auctionId
        -String description
        -Timestamp createdAt
    }

    %% Relationships
    Product "1" --> "0..*" Auction : listed in
    Auction "1" --> "0..*" Bid : contains
    Auction "1" --> "0..*" AutoBid : has active
    User "1" --> "0..*" Bid : places
    User "1" --> "0..*" AutoBid : configures
    User "1" --> "0..*" Transaction : performs
    Auction --> Product : eagerly loaded
```

## Design Decisions & Patterns
    
*   **Singleton DAO Pattern**: All data access objects (`UserDao`, `ProductDao`, `AuctionDao`, etc.) follow the Singleton pattern to ensure global access points for database operations and consistent statement caching if applicable.
*   **Separation of Product and Auction**: `Product` represents a physical asset the user owns. `Auction` is a time-bounded event referencing that product. This 1-to-many relationship allows a product to be re-auctioned after a failed or cancelled session.
*   **Inheritance (IS-A Relationship)**: `User` and `Product` are abstract base classes, enabling polymorphic behavior across specific user roles and product categories.
*   **Factory Method Pattern**: `ItemFactory.createProduct(category, json)` encapsulates instantiation of the correct `Product` subclass at runtime.
*   **Eager Loading**: `Auction` carries a `Product` field that is populated via a SQL JOIN in most queries, avoiding N+1 issues in the auction list and detail views.
*   **Optimistic Locking**: The `version` field on `Auction` supports legacy optimistic-lock checks. Active concurrency control is enforced via pessimistic row-level locking (`SELECT ... FOR UPDATE`) at the database level.
*   **Escrow Model**: `Member` holds two balance fields — `balance` (total funds) and `blockedBalance` (funds reserved for active bids). Available funds = `balance − blockedBalance`. The database enforces `balance >= blocked_balance` at all times.