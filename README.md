# demo-order-system

A demo e-commerce order management system built for educational purposes — used to illustrate Test Automation concepts, testing strategies, and software design patterns.

## Services

| Service           | Port | Description                                      |
|-------------------|------|--------------------------------------------------|
| `order-engine`    | 8080 | Manages order lifecycle and persists to Postgres |
| `payment-service` | 8081 | Stateless payment processing service             |

## Prerequisites

- Java 17
- Maven 3.9+
- Docker & Docker Compose

## Getting Started

### Option A — Full stack via Docker (recommended for manual testing / E2E)

```bash
docker-compose up -d --build
```

Starts PostgreSQL, order-engine (port 8080), and payment-service (port 8081).

### Option B — DB only + local Spring Boot (for active development)

```bash
docker-compose up -d postgres   # start only the DB
cd order-engine && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
```

### Stop everything

```bash
docker-compose down
```

## Testing

### Layer 1 — Unit Tests
- **What they test:** Individual classes and methods in isolation — no Spring context, no database, no network. Example: `OrderStateMachine` transition logic.
- **Tools:** JUnit 5, AssertJ
- **Run:**
  ```bash
  cd order-engine && mvn test -Dgroups=unit
  ```

### Layer 2 — Integration Tests
- **What they test:** The full Spring application context (controllers → service → repository) against a real PostgreSQL database. No real HTTP server is started — `@SpringBootTest(webEnvironment = MOCK)` is used so requests are dispatched in-process via MockMvc (no TCP, no Tomcat). External HTTP calls to payment-service and stock-service are mocked with Mockito.
- **Tools:** JUnit 5, `@SpringBootTest(webEnvironment = MOCK)`, MockMvc, Testcontainers (PostgreSQL), Mockito
- **Requirement:** Docker must be running (Testcontainers starts the DB automatically)
- **Run:**
  ```bash
  cd order-engine && mvn test -Dgroups=integration
  ```

### Run all tests
```bash
cd order-engine && mvn test
```

### Layer 3 — E2E Tests
- **What they test:** Full business workflows end-to-end — all services running, communicating over real HTTP. Validates complete order lifecycle flows across service boundaries.
- **Tools:** To be defined (candidates: REST Assured, Karate)
- **Requirement:** All services and the database must be running
- **Status:** Not yet implemented — planned for a future phase
- **Run:**
  ```bash
  docker-compose up -d --build
  ```
