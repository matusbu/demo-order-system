# CLAUDE.md — demo-order-system

## Project Purpose

This is an educational demo system built for a **Test Automation YouTube channel**.
It is used to illustrate testing strategies, the testing pyramid, and design patterns
in the context of a realistic Java microservices application.

## Monorepo Structure

```
demo-order-system/
├── order-engine/       # Handles order lifecycle: creation, validation, and state transitions.
│                       # Persists orders to PostgreSQL. Contains the order state machine.
├── payment-service/    # Handles payment processing requests from the order engine.
│                       # Stateless service; no direct database connection.
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
