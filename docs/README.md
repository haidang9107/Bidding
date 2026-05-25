# 🏛 Bidding System Documentation

Welcome to the comprehensive technical documentation for the Bidding System. This directory serves as the definitive guide to understanding the system's architecture, data models, workflows, and core design principles.

## 📑 Table of Contents

| Documentation | Description |
| :--- | :--- |
| **[1. System Overview & Architecture](./SystemOverview.md)** | A high-level view of the multi-module project structure, the Real-time Networking Layer, and the Event-Driven Engine. |
| **[2. Core Domain Model Class Diagram](./ClassDiagram_Domain.md)** | Object-oriented design of the business entities (`User`, `Auction`, `Bid`, `Product`) and their relationships. |
| **[3. Server Architecture Class Diagram](./ClassDiagram_Server.md)** | Internal structure of the backend server, highlighting the Command Pattern and NIO thread management. |
| **[4. Database Schema (ERD)](./DatabaseSchema.md)** | Comprehensive Entity-Relationship Diagram outlining tables, relationships, and strict database-level constraints. |
| **[5. Request Processing Sequence Diagram](./SequenceDiagram_Request.md)** | End-to-end trace of a client request, from the TCP socket down to the database transaction and back. |
| **[6. System Use Case Diagram](./UseCaseDiagram.md)** | Business requirements, actors (Members, Admins, System Timer), and their available interactions. |

## 🛠 Technology Stack & Core Patterns

The Bidding System relies on robust engineering patterns to guarantee speed, consistency, and reliability:

*   **Java NIO (Non-blocking I/O)**: Enables the server to handle thousands of concurrent TCP connections with minimal overhead.
*   **Event-Driven Architecture**: Utilizes an internal Publish-Subscribe pattern to decouple domain logic from network broadcasting, ensuring real-time UI updates without blocking the main thread.
*   **Command Pattern**: Maps incoming JSON requests to dedicated command handlers, ensuring the codebase remains modular and strictly adheres to the Single Responsibility Principle.
*   **Optimistic Locking & Transaction Management**: Prevents race conditions during simultaneous bidding, ensuring absolute financial and data integrity.
*   **Mermaid.js**: All diagrams within this documentation are rendered using Mermaid, ensuring they are version-controlled alongside the code and easy to update.

---
*Generated and maintained by the engineering team to ensure architectural clarity.*
