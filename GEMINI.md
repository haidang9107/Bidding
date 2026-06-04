# Project Bidding - Technical Standards

## Tech Stack
- **Java Version:** 21 (LTS)
- **Framework:** Pure Java (Manual Dependency Injection, No Spring Boot)
- **Module Structure:** Multi-module (common, server, client)
- **Communication:** Socket-based with JSON (GSON 2.11.0)
- **Socket Port:** 8888
- **UI:** JavaFX 21.0.6 (Client module)

## Architectural Patterns
- **Services:** Instantiated once in `ServerApp` (Manual DI).
- **DAOs:** Singleton pattern (`getInstance()`).
- **Bidding:** Strategy pattern (`BidStrategy`).

## Communication Protocol
- All messages use `org.example.payload.Request` and `Response`.
- JSON conversion must use `org.example.util.JsonConverter`.
- Each JSON message must end with a newline character (`\n`) for `BufferedReader.readLine()`.
