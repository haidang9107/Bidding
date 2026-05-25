# Server Architecture Class Diagram

This document illustrates the structural architecture of the server-side application. It emphasizes the network layer, request dispatching, and core service components.

## 1. Architectural Components

The server architecture is modularized into several distinct responsibilities:
*   **Networking Layer**: Manages asynchronous TCP connections using Java NIO (`SocketServer`, `DisconnectionHandler`).
*   **Command Dispatching**: Implements the Command Pattern to decouple network requests from business logic (`CommandHandler`, `CommandRegistry`, `Command` interface).
*   **Business Services**: Contains the core logic for bidding, product management, and auction lifecycle (`BidService`, `ProductService`).
*   **Event-Driven Subsystem**: Handles asynchronous, non-blocking internal notifications and broadcast updates (`EventPublisher`, `AuctionMonitor`).

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

    class DisconnectionHandler {
        <<Utility>>
        +handle(SocketChannel channel)
        -cleanupResources(SocketChannel channel)
    }

    %% Event & Lifecycle Management
    class AuctionMonitor {
        -ScheduledExecutorService scheduler
        -Map~Integer, ScheduledFuture~ scheduledTasks
        +start()
        +scheduleAuctionEnd(int auctionId, Timestamp endTime)
        +cancelScheduledEnd(int auctionId)
    }

    class EventPublisher {
        -ExecutorService executorService
        -Map~Class, List~ listeners
        +publish(DomainEvent event)
        +subscribe(Class type, EventListener listener)
    }

    %% Core Services
    class BidService {
        -AuctionMonitor auctionMonitor
        -EventPublisher eventPublisher
        +placeBid(BidRequest request)
        +registerAutoBid(AutoBidRequest request)
    }

    class ProductService {
        -AuctionMonitor auctionMonitor
        -EventPublisher eventPublisher
        +startAuction(AuctionRequest request)
        +processAuctionEnd(int auctionId)
    }

    %% Command Pattern
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

    class LoginCommand {
        +execute(Request request, SocketChannel channel) Response
    }

    class BidPlaceCommand {
        -BidService bidService
        +execute(Request request, SocketChannel channel) Response
    }

    %% Relationships
    SocketServer ..> CommandHandler : spawns
    SocketServer ..> DisconnectionHandler : uses on error/close
    
    CommandHandler --> CommandRegistry : routes request
    CommandRegistry *-- Command : contains
    
    Command <|.. LoginCommand : implements
    Command <|.. BidPlaceCommand : implements
    
    BidPlaceCommand --> BidService : invokes
    
    BidService --> AuctionMonitor : schedules/updates ends
    BidService --> EventPublisher : publishes (e.g., NewBidPlacedEvent)
    
    ProductService --> AuctionMonitor : schedules end time
    AuctionMonitor --> ProductService : triggers end process via callback
```

## 3. Key Design Patterns

*   **Reactor Pattern (NIO)**: `SocketServer` uses a `Selector` to multiplex incoming network events, allowing a single thread to manage thousands of concurrent connections efficiently.
*   **Command Pattern**: By encapsulating requests as `Command` objects, the server easily routes raw JSON to specific business handlers without giant `switch` statements.
*   **Observer/Publish-Subscribe Pattern**: `EventPublisher` decouples services. For instance, `BidService` doesn't need to know how to notify users; it simply publishes a `NewBidPlacedEvent`, and specialized listeners broadcast it.
