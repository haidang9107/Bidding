# Core Domain Model Architecture

This document outlines the core domain model of the Bidding System, detailing the primary entities, their attributes, and the relationships that drive the business logic of the application.

## 1. Domain Entities Overview

The system is built around several key entities:
*   **User Hierarchy**: Represents the participants in the system, utilizing inheritance to distinguish between regular `Member`s and administrative `Admin`s.
*   **Product Hierarchy**: Represents the items being auctioned, with specific subtypes for various categories (e.g., `Electronics`, `Vehicle`).
*   **Bidding Mechanics**: Entities like `Bid` and `AutoBid` manage the transactional interactions within an auction.
*   **Financials**: The `Transaction` entity records all financial movements, ensuring auditability and balance integrity.

## 2. Class Diagram

The following Mermaid class diagram illustrates the object-oriented design of the domain layer.

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
        +banUser(User user)
        +cancelAuction(Auction auction)
    }

    class Member {
        -long balance
        -long blockedBalance
        +placeBid(Auction auction, long amount)
        +topUp(long amount)
        +getAvailableBalance() long
    }

    User <|-- Admin
    User <|-- Member

    %% Product Hierarchy
    class Product {
        <<abstract>>
        -int productId
        -int auctionId
        -String name
        -String description
        -String imageUrl
        -String ownerAccountname
        -boolean inAuction
        -Timestamp withdrawnAt
        -long startingPrice
        -long currentPrice
        -long stepPrice
        -Long buyNowPrice
        -String sellerAccountname
        -String winnerAccountname
        -ItemCategory category
        -AuctionStatus status
        -Timestamp startTime
        -Timestamp endTime
        -int version
        +getters()
        +setters()
    }

    class Electronics {
        -String brand
        -Integer warrantyMonths
    }

    class Vehicle {
        -String model
        -Integer manufactureYear
    }

    class Art {
        -String artist
        -String artType
    }

    class OtherItem {
        -String itemType
    }

    Product <|-- Electronics
    Product <|-- Vehicle
    Product <|-- Art
    Product <|-- OtherItem

    %% Auction Mechanics
    class Bid {
        -int productId
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
    User "1" --> "*" Bid : places
    Product "1" --> "*" Bid : receives
    User "1" --> "*" AutoBid : configures
    Product "1" --> "*" AutoBid : allows
    User "1" --> "*" Transaction : performs
```

## 3. Design Decisions & Patterns

*   **Inheritance (IS-A Relationship)**: The `User` and `Product` classes are designed as abstract base classes. This allows for polymorphic behavior and shared attributes across all specific user roles and product categories.
*   **Composition (HAS-A Relationship)**: `User` and `Product` associate with `Bid`, `AutoBid`, and `Transaction` via one-to-many relationships.
*   **Optimistic Locking**: The `version` attribute in `Product` (representing the Auction state) is crucial for optimistic concurrency control, preventing race conditions during simultaneous bidding.
