# Test Strategy — demo-order-system

## Overview

This document defines the test levels for the demo-order-system monorepo, what each level covers, what it deliberately excludes, and what tooling it uses. It is intended as the reference for the implementation phase.

The system consists of three Spring Boot services (`order-engine`, `payment-service`, `stock-service`) and a React frontend (`order-ui`). Services communicate via synchronous REST calls and asynchronous webhooks; the frontend receives live updates over WebSocket (STOMP/SockJS).

---

## How the Levels Relate

```
Level 1 — Unit          Fast; covers all logic branches. No I/O, no framework startup.
    ↓ fills gap: real DB, Spring wiring, JPA mappings, transaction behaviour
Level 2 — Integration   order-engine with real Postgres + stubbed HTTP partners.
    ↓ fills gap: async coordination, real scheduler, real STOMP delivery
Level 3 — E2E           Full Docker Compose stack, end-to-end flows.
    ↓ fills gap: UI component rendering and WebSocket hook behaviour
Level 4 — Frontend      React components in isolation, no running backend.
```

> **Payload shape safety:** Contract-level tests are intentionally omitted. Inter-service DTOs will be extracted into a shared Maven module (`api-contracts`) in a future refactor — the compiler will then enforce compatibility between services.

---

## Level 1 — Unit Tests

**Purpose:** Verify isolated logic. All I/O and framework infrastructure replaced by mocks.

### Covers
- `OrderStateMachine` — all valid and invalid transitions
- `OrderService` — every method's branching logic and downstream delegation
- `OrderController` + `WebhookController` — HTTP routing, request validation, response shapes via `@WebMvcTest`
- `PaymentTimeoutService` + `PaymentController` — timeout scheduling, cancellation, simulation endpoints
- `StockService` + `StockController` + `SimulateController` — reservation/shipment scheduling, simulation endpoints
- `GlobalExceptionHandler` — ProblemDetail response shapes for `OrderNotFoundException`, `IllegalStateTransitionException`, and `@Valid` failures

### Excludes
- Real database — `OrderRepository` is always mocked
- Real network — `IntegrationClient` and `OrderEngineClient` are always mocked
- WebSocket delivery — `SimpMessagingTemplate.convertAndSend` is verified to be called with correct arguments, but no STOMP subscriber receives the frame
- Real thread scheduling — `ScheduledExecutorService` is mocked; no actual delays fire

### Tooling
| Tool | Role |
|---|---|
| JUnit 5 | Test runner |
| Mockito (`@ExtendWith(MockitoExtension.class)`) | Mocking for pure service tests |
| `@WebMvcTest` + `MockMvc` | Controller-layer tests (HTTP routing, validation, serialisation) |
| AssertJ | Fluent assertions |

---

## Level 2 — Integration Tests

**Purpose:** Boot the full `order-engine` Spring context against a real PostgreSQL container and stubbed HTTP partners. Verify that the DB, Spring wiring, JPA mappings, and transaction behaviour work correctly together.

Integration tests are written for `order-engine` only. `payment-service` and `stock-service` are stateless in-memory services with thin logic — they are already well-covered at Level 1, and their interesting integration surface (webhook delivery to order-engine) is verified at Level 3.

### Covers
- `OrderRepository` — custom queries, result ordering, persistence round-trips
- `OrderController` — full create / read / cancel cycle with DB persistence verified after each call
- `WebhookController` — stock and payment webhooks drive state machine transitions and persist the new status
- `GlobalExceptionHandler` — real 404 / 400 / 422 ProblemDetail response shapes through the live context
- Transaction behaviour — whether a downstream HTTP failure rolls back the DB write or not (a class of bug that unit tests cannot detect)

### Excludes
- `payment-service` and `stock-service` internals — both are replaced by WireMock stubs
- Real asynchronous webhook callbacks — webhook endpoints are invoked directly in tests; the async scheduling in payment/stock is not exercised
- WebSocket frame delivery to a real STOMP subscriber — covered at Level 3
- All frontend code

### Tooling
| Tool | Role |
|---|---|
| `@SpringBootTest(webEnvironment = RANDOM_PORT)` | Full Spring context on a real HTTP port |
| Testcontainers `PostgreSQLContainer` + `@ServiceConnection` | Real PostgreSQL instance; datasource wired automatically |
| WireMock (`wiremock-spring-boot`) | Stubs for `payment-service` and `stock-service` HTTP endpoints |
| `TestRestTemplate` | HTTP client for driving requests in tests |
| Awaitility | Polling assertions where needed |

**Requires Docker** — Testcontainers starts a real PostgreSQL container at test runtime.

New test-scope dependencies to add to `order-engine/pom.xml`:
- `org.testcontainers:postgresql`
- `org.wiremock.integrations:wiremock-spring-boot`

### Target count
15–25 tests.

---

## Level 3 — E2E Tests

**Purpose:** Verify complete business flows through the live Docker Compose stack — from the first REST call to the final persisted status, including WebSocket push notifications.

### Covers
- Happy path (stock first): `CREATED → RESERVED → READY_TO_SHIP → SHIPPING → DELIVERED`
- Happy path (payment first): `CREATED → PAID → READY_TO_SHIP → SHIPPING → DELIVERED`
- Compensation — releasing reservation: `RESERVED → RELEASING_RESERVATION → CANCELLED`
- Compensation — returning payment: `PAID → RETURNING_PAYMENT → CANCELLED`
- Early cancellation: `CREATED → CANCELLED` (stock sold out or immediate user cancel)
- WebSocket frame sequence — STOMP frames arrive in the correct status order for each flow

### Excludes
- State-machine edge cases — all 15 valid and invalid transitions are exhaustively covered at Level 1
- Input validation details — covered at Level 1 and Level 2
- Frontend UI interaction — covered at Level 4
- Failure recovery and resilience scenarios (out of scope for this educational project)

### Tooling
| Tool | Role |
|---|---|
| JUnit 5 | Test runner |
| RestAssured | Fluent HTTP client for driving REST calls |
| `WebSocketStompClient` + `SockJsClient` | STOMP subscriber (same stack as the real UI) |
| Awaitility | Polls `GET /orders/{id}` until expected status is reached; avoids `Thread.sleep` |
| Testcontainers `DockerComposeContainer` | Manages the full stack lifecycle from Java |

**Run gate:** Tagged `@Tag("e2e")`. Excluded from `mvn test`. Run via a dedicated Maven profile `-Pe2e` or a separate `e2e-tests/` module.

### Target count
6–10 tests — one per business flow listed above.

---

## Level 4 — Frontend Component Tests

**Purpose:** Verify React component behaviour in isolation — rendering, WebSocket-driven updates, and interaction logic — without a running backend.

### Covers
- `OrdersPage` — correct badge label and CSS class for all 9 `OrderStatus` values
- `OrdersPage` WebSocket subscription — a simulated STOMP frame triggers an in-place status update without a page reload
- Cancel button visibility — present for `CREATED`, `RESERVED`, `PAID`; absent for all other statuses
- `orderApi.ts` — correct URL, HTTP method, and request body for each API call

### Excludes
- Real WebSocket/STOMP protocol — the `@stomp/stompjs` client is mocked via `vi.mock`
- Visual styling accuracy
- Backend integration — covered at Level 3
- `ShopPage` product list — static demo data, not meaningful to test

### Tooling
| Tool | Role |
|---|---|
| Vitest | Test runner, native Vite integration |
| React Testing Library | Component rendering from the user's perspective |
| `@testing-library/user-event` | Simulates real user interactions (clicks, typing) |
| `@testing-library/jest-dom` | DOM assertion matchers |
| jsdom | Browser environment for Node |
| `vi.mock` | Mocks axios and `@stomp/stompjs` at the module boundary |

New devDependencies to add to `order-ui/package.json`:
- `vitest`
- `@testing-library/react`
- `@testing-library/user-event`
- `@testing-library/jest-dom`
- `jsdom`

### Target count
12–20 tests.

---

## Build / CI Organisation

| Level | Tag / convention | Maven / npm phase | When to run |
|---|---|---|---|
| 1 — Unit | `@Tag("unit")` | `mvn test` (Surefire) | Every commit |
| 2 — Integration | `@Tag("integration")`, `*IT.java` | `mvn verify` (Failsafe) | Every PR |
| 3 — E2E | `@Tag("e2e")` | `mvn verify -Pe2e` | Pre-merge gate or nightly |
| 4 — Frontend | — | `npm run test` (Vitest) | Every commit |
