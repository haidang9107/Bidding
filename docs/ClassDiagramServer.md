# Server Architecture Class Diagram

This document illustrates the structural architecture of the server-side application. It emphasises the network layer, request dispatching, and the core service components that drive the auction lifecycle.

## 1. Architectural Components

The server is divided into several distinct layers:
*   **Networking Layer**: Manages asynchronous TCP connections using Java NIO (`SocketServer`, `DisconnectionHandler`, `HeartbeatRegistry`, `InactivityMonitor`).
*   **Command Dispatching**: Implements the Command Pattern to route raw JSON requests to typed business handlers (`CommandHandler`, `CommandRegistry`, `Command` interface).
*   **Business Services**: Contains domain logic split by responsibility — `AuctionService` (lifecycle), `BidService` (bid placement & auto-bidding), `ProductService` (inventory), finance services (`DepositService`, `WithdrawService`, `TransferService`), and user services (`AuthService`, `UserService`, `AdminService`).
*   **Event-Driven Subsystem**: Decouples domain state changes from network broadcasting via an internal Pub/Sub bus (`EventPublisher`, `NetworkNotificationListener`, `Broadcaster`).
*   **Persistence Layer**: `TransactionManager` wraps every mutation in a JDBC transaction; individual DAOs (`AuctionDao`, `BidDao`, `ProductDao`, `UserDao`, `AutoBidDao`, `TransactionDao`) handle SQL.

## 2. Server Class Diagram

```mermaid
classDiagram
    %% Network Layer
    class SocketServer {
        -Selector selector
        -ExecutorService executorService
        -Map~SocketChannel, ByteBuffer~ clientBuffers
        +run()
        -handleAccept(SelectionKey key)
        -handleRead(SelectionKey key)
        -closeChannel(SelectionKey key)
    }

    class CommandHandler {
        -SocketChannel clientChannel
        -CommandRegistry commandRegistry
        -String rawJsonRequest
        +run()
        -dispatch()
    }

    class CommandRegistry {
        -Map~String, Command~ commands
        +getCommand(String type) Command
        +register(String type, Command cmd)
    }

    class Command {
        <<interface>>
        +execute(Request request, SocketChannel channel) Response
    }

    class DisconnectionHandler {
        <<Utility>>
        +handle(SocketChannel channel)
    }

    class HeartbeatRegistry {
        +recordActivity(SocketChannel channel)
        +getLastSeen(SocketChannel channel) long
    }

    class InactivityMonitor {
        -HeartbeatRegistry heartbeatRegistry
        +start()
        -checkAndEvict()
    }

    %% Auction Lifecycle
    class AuctionMonitor {
        -ScheduledExecutorService scheduler
        -Map~Integer, ScheduledFuture~ scheduledTasks
        -AuctionService auctionService
        +start()
        +scheduleAuctionStart(int auctionId, Timestamp startTime)
        +scheduleAuctionEnd(int auctionId, Timestamp endTime)
        +stop()
    }

    class AuctionService {
        -AuctionDao auctionDao
        -ProductDao productDao
        -UserDao userDao
        -TransactionDao transactionDao
        -ProductService productService
        -TransactionManager txManager
        -EventPublisher eventPublisher
        -AuctionMonitor auctionMonitor
        +createAuction(ProductAddRequest req, String seller)
        +openAuctionForProduct(int productId, ...)
        +startAuction(int auctionId)
        +finishAuction(int auctionId)
        +cancelAuction(int auctionId) boolean
        +processExpiredAuctions()
        +processUpcomingAuctions()
        +getAuctionById(int auctionId) Auction
        +getAuctionsPaged(int page, int size) PagedResponse
    }

    %% Bid Services
    class BidService {
        -AuctionDao auctionDao
        -BidDao bidDao
        -UserDao userDao
        -AutoBidDao autoBidDao
        -TransactionManager txManager
        -EventPublisher eventPublisher
        -AuctionMonitor auctionMonitor
        +placeBid(int auctionId, String bidder, long amount) BidResult
        +configureAutoBid(int auctionId, String bidder, long maxBid, long increment)
        +cancelAutoBid(int auctionId, String bidder)
        +getBidHistoryPaged(int auctionId, int page, int size) PagedResponse
    }

    class AntiSnipping {
        <<Utility>>
        -long SNIP_WINDOW_MS
        -long EXTENSION_MS
        +process(Connection conn, Auction auction, AuctionDao dao)
    }

    %% Product & User Services
    class ProductService {
        -ProductDao productDao
        -TransactionManager txManager
        +createInventoryProduct(ProductCreateRequest req, String owner)
        +getProductsByOwner(String owner) List
        +transferOwnership(Connection conn, int productId, String newOwner)
    }

    %% Event System
    class EventPublisher {
        -ExecutorService executorService
        -Map~Class, List~ listeners
        +publish(DomainEvent event)
        +subscribe(Class type, EventListener listener)
    }

    class NetworkNotificationListener {
        +onEvent(DomainEvent event)
    }

    class Broadcaster {
        -SessionManager sessionManager
        -RoomManager roomManager
        +broadcastToRoom(int auctionId, String json)
        +sendToSession(SocketChannel channel, String json)
    }

    %% Persistence
    class TransactionManager {
        -DatabaseConnectionPool pool
        +run(TxConsumer action)
        +execute(TxFunction action) T
        +query(TxFunction action) T
    }

    %% Relationships
    SocketServer ..> CommandHandler : spawns per request
    SocketServer ..> DisconnectionHandler : on close/error
    SocketServer --> HeartbeatRegistry : records activity
    InactivityMonitor --> HeartbeatRegistry : reads last seen

    CommandHandler --> CommandRegistry : resolves Command
    CommandRegistry *-- Command : registers

    Command <|.. BidPlaceCommand : implements
    Command <|.. AuctionCreateCommand : implements
    Command <|.. LoginCommand : implements

    BidPlaceCommand --> BidService : invokes
    AuctionCreateCommand --> AuctionService : invokes

    BidService --> AuctionMonitor : reschedule on anti-snip
    BidService --> EventPublisher : publish(NewBidPlacedEvent)
    BidService --> AntiSnipping : process()

    AuctionService --> AuctionMonitor : scheduleStart / scheduleEnd
    AuctionService --> EventPublisher : publish(AuctionEndedEvent, ...)
    AuctionService --> ProductService : transferOwnership()
    AuctionMonitor --> AuctionService : triggers startAuction / finishAuction

    EventPublisher --> NetworkNotificationListener : dispatch async
    NetworkNotificationListener --> Broadcaster : send JSON

    AuctionService --> TransactionManager : txManager.run()
    BidService --> TransactionManager : txManager.execute()
```

## 3. Key Design Patterns

*   **Reactor Pattern (NIO)**: `SocketServer` uses a `Selector` to multiplex I/O events across thousands of concurrent TCP connections on a small thread pool.
*   **Command Pattern**: Each JSON `type` field maps to a dedicated `Command` implementation, isolating request parsing from business logic and eliminating large `switch` blocks.
*   **Publish-Subscribe (Observer)**: `EventPublisher` dispatches `DomainEvent`s asynchronously. `BidService` and `AuctionService` publish events without any knowledge of how clients are notified.
*   **Circular Dependency Resolution**: `AuctionService` and `AuctionMonitor` depend on each other. The dependency is broken by injecting `AuctionMonitor` via `AuctionService.setAuctionMonitor()` after both objects are constructed in `ServerApp`.
*   **Template Method (TransactionManager)**: `txManager.run()`, `execute()`, and `query()` provide a uniform transactional wrapper, ensuring every DB mutation is automatically committed or rolled back.