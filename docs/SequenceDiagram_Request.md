# Request Processing Sequence Diagram

This sequence diagram illustrates the lifecycle of a single request originating from the Client, traversing through the Server's networking and business logic layers, updating the Database, and finally triggering asynchronous real-time notifications to all connected clients.

## 1. End-to-End Execution Flow

The flow is divided into two distinct phases:
1.  **Synchronous Request-Response Cycle**: The immediate processing of the user's action (e.g., placing a bid) and the direct response indicating success or failure.
2.  **Asynchronous Notification Cycle**: The decoupled background process that broadcasts the state change to all interested parties (e.g., updating the UI of everyone watching the auction).

## 2. Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    
    actor User
    participant ClientApp as Client (JavaFX)
    participant SocketServer as TCP Server (NIO)
    participant CommandHandler as Command Dispatcher
    participant Service as Business Service Layer
    participant Database as MySQL (HikariCP)
    participant EventPublisher as Event Bus
    participant AsyncThread as Notification ThreadPool
    
    %% Phase 1: Synchronous Handling
    Note over User, ClientApp: 1. Synchronous Request-Response Phase
    User->>ClientApp: Interacts with UI (e.g., clicks "Bid")
    ClientApp->>SocketServer: Transmit JSON Payload over TCP
    
    SocketServer->>CommandHandler: Route to Worker Thread
    activate CommandHandler
    
    CommandHandler->>Service: Dispatch Request (e.g., BidPlaceCommand)
    activate Service
    
    Service->>Database: BEGIN TRANSACTION
    activate Database
    Service->>Database: Validate & Update (e.g., UPDATE auctions SET...)
    Database-->>Service: COMMIT (Success)
    deactivate Database
    
    %% Triggering the Event
    Service->>EventPublisher: publish(NewBidPlacedEvent)
    activate EventPublisher
    EventPublisher-->>Service: Return Immediately (Non-blocking)
    deactivate EventPublisher
    
    Service-->>CommandHandler: Return Success Response Object
    deactivate Service
    
    CommandHandler->>ClientApp: Transmit JSON Response
    deactivate CommandHandler
    ClientApp-->>User: Update local UI state
    
    %% Phase 2: Asynchronous Broadcast
    Note over EventPublisher, ClientApp: 2. Asynchronous Notification Phase
    EventPublisher->>AsyncThread: Submit Event to Background Pool
    activate AsyncThread
    
    AsyncThread->>AsyncThread: Process Listeners (NetworkNotificationListener)
    AsyncThread->>SocketServer: Generate Broadcast Message
    
    par [Broadcast to all relevant clients]
        SocketServer-->>ClientApp: Send Real-time JSON Update (Client 1)
    and [Client 2]
        SocketServer-->>ClientApp: Send Real-time JSON Update (Client 2)
    and [Client N]
        SocketServer-->>ClientApp: Send Real-time JSON Update (Client N)
    end
    
    ClientApp-->>User: Refresh View (e.g., New highest bid shown)
    deactivate AsyncThread
```

## 3. Key Takeaways

*   **Non-Blocking I/O**: The `EventPublisher` returns immediately, ensuring the user placing the bid receives their confirmation without waiting for the server to notify hundreds of other users.
*   **Database Transactions**: Critical operations are wrapped in ACID-compliant transactions, ensuring that if anything fails (e.g., insufficient funds), the rollback happens safely before any success response or event is emitted.
*   **Scalable Broadcasting**: By delegating the broadcasting task to an `AsyncThread`, the server's primary request-handling threads remain free to process new incoming commands.
