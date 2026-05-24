# Core Domain Model Class Diagram

```mermaid
classDiagram
    class User {
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
        +banUser()
        +cancelAuction()
    }

    class Member {
        -long balance
        -long blockedBalance
        +placeBid()
        +topUp()
    }

    User <|-- Admin
    User <|-- Member

    class Item {
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

    Item <|-- Electronics
    Item <|-- Vehicle
    Item <|-- Art
    Item <|-- OtherItem

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
        -Integer referenceId
        -String description
        -Timestamp createdAt
    }

    User "1" -- "*" Bid : places
    Item "1" -- "*" Bid : has
    User "1" -- "*" AutoBid : configures
    Item "1" -- "*" AutoBid : has
    User "1" -- "*" Transaction : performs
```
