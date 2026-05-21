# Project Bidding - Technical Standards

## Tech Stack
- **Java Version:** 25 (LTS)
- **Framework:** Spring Boot 4.0.6 (Supports Java 25)
- **Module Structure:** Multi-module (common, server, client)
- **Communication:** Socket-based with JSON (GSON 2.11.0)
- **Socket Port:** 8888
- **UI:** JavaFX 21.0.6 (Client module)

## Communication Protocol
- All messages use `org.example.payload.Request` and `Response`.
- JSON conversion must use `org.example.util.JsonConverter`.
- Each JSON message must end with a newline character (`\n`) for `BufferedReader.readLine()`.

## Environment Notes
- **macOS Docker:** If using Docker Desktop, ensure the socket is correctly mapped or use Spring Boot 4.0.6+ for better compatibility.
