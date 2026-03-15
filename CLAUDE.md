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
│                       # Exposes a STOMP/WebSocket endpoint at /ws/orders (SockJS fallback)
│                       # that pushes order status updates to /topic/orders/{customerName}.
├── payment-service/    # Handles payment processing requests from the order engine.
│                       # Holds in-memory timeout state per order; no persistent storage.
├── stock-service/      # Gateway to the internal warehouse and shipping system.
│                       # Exposes simulation endpoints for manual triggering of warehouse
│                       # and shipping outcomes. Holds in-memory reservation/shipment state;
│                       # no persistent storage.
├── order-ui/           # React + TypeScript + Vite frontend. Three pages: Login,
│                       # Shop (5 Apple products), and My Orders. Communicates with
│                       # order-engine via REST (axios) and WebSocket (STOMP/SockJS).
│                       # Served by nginx on port 3000; nginx proxies /api/* to order-engine.
├── docker-compose.yml  # Runs PostgreSQL 16, all three services, and order-ui.
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
