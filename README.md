# demo-order-system

A demo e-commerce order management system built for educational purposes — used to illustrate Test Automation concepts, testing strategies, and software design patterns.

## Services

| Service           | Port | Description                                                         | Swagger UI                            |
|-------------------|------|---------------------------------------------------------------------|---------------------------------------|
| `order-engine`    | 8080 | Manages order lifecycle and persists to PostgreSQL                  | http://localhost:8080/swagger-ui.html |
| `payment-service` | 8081 | Handles payment processing; holds in-memory timeout state per order | http://localhost:8081/swagger-ui.html |
| `stock-service`   | 8082 | Warehouse & shipping gateway with simulation endpoints              | http://localhost:8082/swagger-ui.html |
| `order-ui`        | 3000 | React frontend — Login, Shop, My Orders (live WS updates)          | —                                     |

## Tech Stack

| Layer    | Technology                         |
|----------|------------------------------------|
| Backend  | Java 17, Spring Boot 3, Maven      |
| Database | PostgreSQL 16                      |
| Frontend | React 19, TypeScript, Vite, nginx  |
| Runtime  | Docker / Docker Compose            |

## Prerequisites

- Java 17
- Maven 3.9+
- Docker & Docker Compose
- Node.js 20+ (only needed for local frontend development)

## Getting Started

### Option A — Full stack via Docker (recommended for manual testing / E2E)

```bash
docker-compose up -d --build
```

Starts PostgreSQL, order-engine (8080), payment-service (8081), stock-service (8082), and order-ui (http://localhost:3000).

### Option B — DB only + local Spring Boot (for active development)

```bash
docker-compose up -d postgres   # start only the DB
cd order-engine && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd stock-service && mvn spring-boot:run
```

### Stop everything

```bash
docker-compose down
```

## Testing

See [`test-strategy.md`](test-strategy.md) for full scope, tooling, and design decisions.

| Level | What | Run command |
|-------|------|-------------|
| 1 — Unit | Isolated logic, no I/O | `mvn test -Dgroups=unit` |
| 2 — Integration | `order-engine` + real Postgres + WireMock (requires Docker) | `mvn verify -Dgroups=integration -pl order-engine` |
| 3 — E2E | Full Docker Compose stack incl. WebSocket (requires Docker) | `mvn verify -Pe2e -pl e2e-tests` |
| 4 — Frontend | React components in isolation | `cd order-ui && npm run test` |
