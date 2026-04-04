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

The project has four test levels. See [`TEST_STRATEGY.md`](TEST_STRATEGY.md) for the full strategy, design decisions, and infrastructure details.

### Layer 1 — Unit Tests
- **Scope:** Individual classes in isolation — no Spring context, no database, no network.
- **Tools:** JUnit 5, Mockito, AssertJ
- **Run:** `mvn test -Dgroups=unit`

### Layer 2 — Integration Tests
- **Scope:** Each service with its full Spring context. External HTTP peers are replaced with `@MockitoBean`. PostgreSQL runs in a TestContainers container for order-engine.
- **Tools:** Spring Boot Test, TestContainers, Mockito
- **Run:** `mvn test -Dgroups=integration`

### Layer 3 — API-level E2E Tests
- **Scope:** All three services running as real Docker containers. No mocking — real HTTP between services. Tests are written using three actors (`Client`, `PaymentSystem`, `WarehouseSystem`) that expose a business-readable DSL.
- **Tools:** TestContainers `DockerComposeContainer`, RestAssured, Awaitility
- **Run:** `mvn test -Dgroups=e2e -pl order-engine` *(requires Docker; builds images on first run)*

### Layer 4 — Playwright GUI Tests *(planned)*
- **Scope:** nginx proxy correctness and WebSocket live updates via a real browser.
- **Tools:** Playwright
- **Run:** `cd order-ui && npx playwright test`
