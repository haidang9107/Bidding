# Request Processing Sequence Diagram

This document traces the lifecycle of two critical flows: a **manual bid** (the most complex synchronous path) and **auction finalization** (the automated timer-driven path).

---

## 1. Flow A — Manual Bid Placement (Synchronous + Async Notify)

Illustrates the end-to-end path from the moment a user clicks "Bid" through database settlement and realtime broadcast to all watchers.

```mermaid
sequenceDiagram
    autonumber

    actor User
    participant ClientApp as Client (JavaFX)
    participant SocketServer as TCP Server (NIO)
    participant CommandHandler as Command Dispatcher
    participant BidPlaceCommand as BidPlaceCommand
    participant BidService as BidService
    participant BidStrategy as BidStrategy (Normal/BuyNow)
    participant AuctionDao as AuctionDao (Singleton)
    participant UserDao as UserDao (Singleton)
    participant BidDao as BidDao (Singleton)
    participant AntiSnipping as AntiSnipping
    participant AuctionMonitor as AuctionMonitor
    participant EventPublisher as EventPublisher
    participant Broadcaster as Broadcaster (async)

    Note over User, ClientApp: Phase 1 — Synchronous Request / Response
    User->>ClientApp: Click "Place Bid"
    ClientApp->>SocketServer: JSON { type: BID_PLACE, auctionId, amount }

    SocketServer->>CommandHandler: Spawn worker thread
    activate CommandHandler
    CommandHandler->>BidPlaceCommand: execute(request, channel)
    activate BidPlaceCommand
    BidPlaceCommand->>BidService: placeBid(auctionId, bidder, amount)
    activate BidService

    BidService->>AuctionDao: AuctionDao.getInstance()
    BidService->>AuctionDao: BEGIN TX — getAuctionForUpdate(auctionId)
    AuctionDao-->>BidService: Auction (row locked)

    BidService->>UserDao: UserDao.getInstance()
    BidService->>UserDao: findByAccountnameForUpdate(bidder)
    BidService->>UserDao: findByAccountnameForUpdate(prevWinner) [if different]
    UserDao-->>BidService: Member objects (rows locked)

    BidService->>BidService: Validate bid amount >= currentPrice + stepPrice
    BidService->>BidService: Check available balance (balance - blockedBalance)

    BidService->>BidStrategy: execute(connection, auction, bidder, amount)
    activate BidStrategy
    alt Buy Now Strategy
        BidStrategy->>AuctionDao: updateAuctionEndTime(now)
    else Normal Strategy
        BidStrategy->>AntiSnipping: process(conn, auction, auctionDao)
        AntiSnipping->>AuctionDao: updateAuctionEndTime(extended) [if in snip window]
    end
    deactivate BidStrategy

    BidService->>UserDao: addBlockedBalance(prevWinner, -currentPrice) [if different]
    BidService->>UserDao: addBlockedBalance(bidder, +extraBlocked)
    BidService->>AuctionDao: updateBidLocked(auctionId, newPrice, bidder)
    
    BidService->>BidDao: BidDao.getInstance()
    BidService->>BidDao: insertBid(auctionId, bidder, amount, isAuto)

    BidService->>BidService: runAutoBidding() [may loop, place counter-bids]

    BidService->>AuctionMonitor: scheduleAuctionEnd(auctionId, newEndTime)
    BidService->>EventPublisher: publish(NewBidPlacedEvent) — COMMIT TX

    BidService-->>BidPlaceCommand: BidResult
    deactivate BidService
    BidPlaceCommand-->>CommandHandler: Response (success)
    deactivate BidPlaceCommand
    CommandHandler->>ClientApp: JSON Response { winner, currentPrice, endTime }
    deactivate CommandHandler
    ClientApp-->>User: Update bid UI

    Note over EventPublisher, Broadcaster: Phase 2 — Async Broadcast (non-blocking)
    EventPublisher->>Broadcaster: dispatch NewBidPlacedEvent to thread pool
    activate Broadcaster
    Broadcaster->>SocketServer: broadcastToRoom(auctionId, BidUpdateNotify JSON)
    SocketServer-->>ClientApp: Push to all watchers in the auction room
    deactivate Broadcaster
```

---

## 2. Flow B — Automated Auction Finalization (Timer-driven)

Triggered by `AuctionMonitor`'s `ScheduledExecutorService` when the auction's `end_time` is reached.

```mermaid
sequenceDiagram
    autonumber

    participant AuctionMonitor as AuctionMonitor (scheduler)
    participant AuctionService as AuctionService
    participant AuctionDao as AuctionDao (FOR UPDATE)
    participant UserDao as UserDao
    participant ProductService as ProductService
    participant TransactionDao as TransactionDao
    participant EventPublisher as EventPublisher
    participant Broadcaster as Broadcaster (async)

    Note over AuctionMonitor: ScheduledFuture fires at end_time
    AuctionMonitor->>AuctionService: processAuctionEnd(auctionId)
    activate AuctionService

    AuctionService->>AuctionDao: BEGIN TX — getAuctionForUpdate(auctionId)
    AuctionDao-->>AuctionService: Auction (row locked, status=RUNNING)

    AuctionService->>AuctionDao: updateStatus(FINISHED)

    alt Has winner
        AuctionService->>UserDao: addBalance(winner, -finalPrice)
        AuctionService->>UserDao: addBlockedBalance(winner, -finalPrice)
        AuctionService->>UserDao: addBalance(seller, +finalPrice)
        AuctionService->>ProductService: transferOwnership(productId, winner)
        AuctionService->>TransactionDao: insertTransaction(AUCTION_PAYMENT)
    else No winner
        AuctionService->>ProductDao: updateProductAuctionFlag(productId, false)
    end

    AuctionService->>EventPublisher: publish(AuctionEndedEvent) — COMMIT TX
    deactivate AuctionService

    EventPublisher->>Broadcaster: dispatch async
    activate Broadcaster
    Broadcaster->>SocketServer: broadcastToRoom(auctionId, AuctionEndNotify JSON)
    SocketServer-->>ClientApp: Notify all watchers (winner, final price, status)
    deactivate Broadcaster
```

---

## 3. Key Design Properties

| Property | Implementation |
|---|---|
| **Non-blocking response** | `EventPublisher.publish()` returns immediately; broadcast runs on a separate thread pool |
| **Deadlock prevention** | Users are always locked in alphabetical order (`bidder.compareTo(prevWinner)`) |
| **Rollback on failure** | `TransactionManager` catches any exception and issues a full `ROLLBACK` before propagating |
| **Anti-snipping** | Any bid inside the final `ANTI_SNIP_WINDOW_MS` extends `end_time` by `ANTI_SNIP_EXTENSION_MS`; `AuctionMonitor` reschedules the closure task accordingly |
| **Auto-bid loop** | After each manual bid, `runAutoBidding()` runs within the same transaction to settle all configured auto-bids before committing |