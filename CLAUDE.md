# CLAUDE.md — demo-order-system

## Project Purpose

A demo e-commerce order management system built for educational purposes — used to
illustrate Test Automation concepts, testing strategies, and software design patterns
in the context of a realistic Java microservices application.

## Monorepo Structure

```
demo-order-system/
├── order-engine/       # Handles order lifecycle: creation, validation, and state transitions.
│                       # Persists orders to PostgreSQL. Contains the order state machine.
├── payment-service/    # Handles payment processing requests from the order engine.
│                       # Holds in-memory timeout state per order; no persistent storage.
├── docker-compose.yml  # Runs the PostgreSQL 16 instance used by order-engine.
├── CLAUDE.md           # This file.
└── README.md           # Project overview and startup instructions.
```

## Tech Stack

| Layer       | Technology                  |
|-------------|-----------------------------|
| Language    | Java 17                     |
| Framework   | Spring Boot 3               |
| Build tool  | Maven                       |
| Database    | PostgreSQL 16               |
| Runtime     | Docker / Docker Compose     |

## Notes

- For the complete order state machine and all valid transitions, see [`docs/state-machine.md`](docs/state-machine.md).
