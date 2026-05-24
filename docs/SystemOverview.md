# Bidding System Overview & Event-Driven Architecture

This document provides a high-level overview of the project structure and the event-driven notification system.

## 🏗 Project Structure

The project is divided into three main modules:

1.  **`common`**: Contains shared data structures, including:
    *   **Models**: User, Item, Auction, Bid, etc.
    *   **DTOs**: Requests, Responses, and Notification payloads.
    *   **Utils**: JSON conversion, Configuration, and Logging.
2.  **`server`**: The core backend logic:
    *   **Network**: Socket-based server using NIO (Selector/Channel).
    *   **Commands**: Implementation of the Command Pattern for handling requests.
    *   **Services**: Business logic for Bidding, Finance, and User management.
    *   **Repository**: Data access layer (DAO) with JDBC.
3.  **`client`**: A client application/test suite that interacts with the server.

## 📢 Event-Driven Architecture (Asynchronous)

The system uses an internal **Asynchronous Event Bus** to decouple business logic from notification logic. This ensures that database transactions are released immediately, even if network delivery is slow.

```mermaid
graph TD
    subgraph "Business Layer (Main Thread)"
        S[Service] -- publishes --> EP[EventPublisher]
    end

    subgraph "Event Bus (Thread Pool)"
        EP -- async dispatch --> NNL[NetworkNotificationListener]
    end

    subgraph "Notification Layer"
        NNL -- uses --> B[Broadcaster]
        B -- sends to --> C1[Client 1]
        B -- sends to --> C2[Client 2]
    end

    E1[NewBidPlacedEvent] -.-> EP
    E2[AuctionEndedEvent] -.-> EP
    E3[ProductCreatedEvent] -.-> EP
```

### Key Components:
*   **`EventPublisher`**: Uses a fixed thread pool to dispatch events asynchronously.
*   **`DomainEvent`**: Base interface for all system events.
*   **`NetworkNotificationListener`**: Subscribes to events and converts them into network responses.
*   **`Broadcaster`**: Manages active socket connections and pushes JSON messages to clients.

## 🕒 Precise Auction Monitoring

The system features a millisecond-accurate auction closure mechanism using a dedicated scheduling system.

```mermaid
graph LR
    BS[BidService] -- triggers --> AM[AuctionMonitor]
    PS[ProductService] -- triggers --> AM
    AM -- schedules task --> SES[ScheduledExecutorService]
    SES -- executed at endTime --> PS
    PS -- publishes --> EP[EventPublisher]
```

*   **Scheduling**: Instead of polling the database, `AuctionMonitor` schedules a precise task for every auction's `endTime`.
*   **Anti-Snipping Integration**: When a last-minute bid extends an auction, the old task is cancelled and a new one is scheduled instantly.
*   **Fault Tolerance**: A background "defensive" task runs every minute to capture any auctions missed due to server restarts.

## 🤝 Component Interaction

1.  **Request**: Client sends a JSON request via Socket.
2.  **Execution**: `CommandHandler` identifies the `Command`, which calls a `Service`.
3.  **State Change**: `Service` updates the database via `Repository`.
4.  **Event**: `Service` publishes a `DomainEvent` (e.g., `NewBidPlacedEvent`).
5.  **Notification**: `NetworkNotificationListener` receives the event and uses `Broadcaster` to update all interested clients in real-time.
