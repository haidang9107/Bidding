# 🏛 Bidding System — Technical Documentation

This directory is the definitive technical reference for the Bidding System. Every diagram is rendered with Mermaid and version-controlled alongside the source code.

## 📑 Table of Contents

| Document | Description |
|:---|:---|
| **[1. System Overview](./SystemOverview.md)** | Multi-module structure, NIO networking, event-driven architecture, concurrency model, and advanced business rules (auto-bid, anti-snipping, buy-now). |
| **[2. Domain Model Class Diagram](ClassDiagramDomain.md)** | OOP design of all business entities — `User`, `Product`, `Auction`, `Bid`, `AutoBid`, `Transaction` — and their relationships. |
| **[3. Server Architecture Class Diagram](ClassDiagramServer.md)** | Internal structure of the backend: NIO layer, Command Pattern, service layer, event bus, and persistence layer. |
| **[4. Database Schema (ERD)](./DatabaseSchema.md)** | Full ERD with all tables, columns, CHECK constraints, FK cascade policies, and index rationale. |
| **[5. Sequence Diagrams](SequenceDiagramRequest.md)** | End-to-end traces for (A) manual bid placement and (B) automated auction finalization. |
| **[6. Use Case Diagram](./UseCaseDiagram.md)** | Business requirements mapped to actors: Member, Admin, and System Timer. |

---

## 🧱 Technology Stack

| Concern | Technology                          |
|---|-------------------------------------|
| Language | Java 25                             |
| Networking | Java NIO (non-blocking TCP sockets) |
| GUI | JavaFX + FXML                       |
| Database | MySQL 9                             |
| Connection pooling | HikariCP                            |
| JSON | Gson                                |
| Password hashing | BCrypt                              |
| Build | Maven (multi-module)                |
| Diagrams | Mermaid.js                          |

---

## 🔑 Core Design Patterns

| Pattern | Where used |
|---|---|
| **Command** | `CommandRegistry` + `Command` interface — maps JSON `type` to handler class |
| **Observer / Pub-Sub** | `EventPublisher` → `NetworkNotificationListener` → `Broadcaster` |
| **Factory Method** | `ItemFactory.createProduct(category, json)` |
| **Singleton (instance-scoped)** | `DatabaseConnectionPool`, `SessionManager`, `RoomManager` — created once in `ServerApp` and injected |
| **Template Method** | `TransactionManager.run / execute / query` |
| **Reactor (NIO)** | `SocketServer` Selector loop |
| **Pessimistic Locking** | `SELECT ... FOR UPDATE` on `auctions` and `users` rows |

---

## 💾 Module Layout

```
project-root/
├── common/          # Shared models, DTOs, enums, utilities
│   └── src/main/java/org/example/
│       ├── model/   # Auction, Product subtypes, Bid, AutoBid, Transaction, User
│       ├── dto/     # Request, Response, Notify DTOs
│       └── util/    # Config, FileLogger, JsonConverter
├── server/          # Backend TCP server
│   └── src/main/java/org/example/server/
│       ├── network/     # SocketServer, CommandHandler, Broadcaster, SessionManager
│       ├── controller/  # Thin adapter between Command and Service
│       ├── service/     # AuctionService, BidService, ProductService, finance, user
│       ├── repository/  # DAOs + TransactionManager + DatabaseConnectionPool
│       ├── event/       # DomainEvent, EventPublisher, listeners
│       └── exception/   # Typed exception hierarchy
├── client/          # JavaFX frontend
└── docs/            # This directory
```

---

*Maintained by the engineering team. Update diagrams alongside any structural code change.*