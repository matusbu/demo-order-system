# Test Strategy — demo-order-system

## Overview

This document defines the full test strategy for the demo-order-system. The system currently has
unit tests only. This strategy adds integration, E2E, and GUI test layers to be implemented
incrementally.

```
  ┌────────────────────────────────────┐
  │  Playwright GUI Tests              │  nginx + WebSocket smoke (3 scenarios)
  ├────────────────────────────────────┤
  │  API-level E2E (actor DSL)         │  full business lifecycle (5 scenarios)
  ├────────────────────────────────────┤
  │  Integration Tests                 │  per-service, real DB, mocked peers
  ├────────────────────────────────────┤
  │  Unit Tests ✓                      │  already implemented
  └────────────────────────────────────┘
```

---

## Level 1 — Unit Tests (already implemented)

**Tag:** `@Tag("unit")`

| Service | Class | What it tests |
|---|---|---|
| order-engine | `OrderStateMachineTest` | All 14 valid state transitions + invalid transition rejection |
| payment-service | `PaymentControllerTest` | Controller endpoint contracts |
| payment-service | `PaymentTimeoutServiceTest` | Timeout scheduling and cancellation |
| stock-service | `SimulateControllerTest` | Simulate controller endpoint contracts |
| stock-service | `StockServiceTest` | Reservation, cancellation, and shipment service logic |

**Run:** `mvn test -Dgroups=unit`

---

## Level 2 — Integration Tests

**Tag:** `@Tag("integration")`

Each service is tested with its full Spring context running. External HTTP peers are replaced
with `@MockitoBean` — no WireMock (Jetty server factory conflicts in Spring Boot 3.4.x).

### order-engine

**Infrastructure:** TestContainers PostgreSQL **singleton** (static block, not `@Container`).
Singleton is required because `@MockitoBean` forces a new Spring context per test class,
which causes HikariCP connection failures when a second container is started.

| Class | What it tests |
|---|---|
| `OrderRepositoryIT` | JPA queries: save, findById, findByCustomerName; UUID primary key; timestamps |
| `OrderServiceIT` | createOrder persists to DB and calls `IntegrationClient`; stock/payment webhooks drive state transitions; cancelOrder triggers correct compensation calls |
| `OrderControllerIT` | `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`: all REST endpoints + 404/422/400 error responses |
| `WebhookControllerIT` | All valid stock and payment webhook events; 422 for invalid state transitions |

**Key assertions:**
- DB row `status` column after each event
- `IntegrationClient` method calls verified with `ArgumentCaptor`
- `ProblemDetail` response bodies on error paths
- Transactional rollback: order status unchanged when transition throws

**Files to create:**
- `order-engine/src/test/java/com/demo/orderengine/repository/OrderRepositoryIT.java`
- `order-engine/src/test/java/com/demo/orderengine/service/OrderServiceIT.java`
- `order-engine/src/test/java/com/demo/orderengine/controller/OrderControllerIT.java`
- `order-engine/src/test/java/com/demo/orderengine/controller/WebhookControllerIT.java`
- `order-engine/src/test/java/com/demo/orderengine/PostgresContainerConfig.java` (singleton base)
- `order-engine/src/test/resources/application-test.yml` (test datasource override)

### payment-service

**Infrastructure:** No DB. `@SpringBootTest(RANDOM_PORT)` + `@MockitoBean OrderEngineClient`.
Override `payment.timeout-minutes` to a very short value in `application-test.yml` so timeout
tests do not wait 5 minutes.

| Class | What it tests |
|---|---|
| `PaymentTimeoutServiceIT` | register → pay → `PAYMENT_RECEIVED` sent; register → timeout fires → `PAYMENT_TIMEOUT` sent; initiate return → `PAYMENT_RETURNED` sent after delay |
| `PaymentControllerIT` | HTTP contracts for all 4 endpoints; side effects on service state |

**Files to create:**
- `payment-service/src/test/java/com/demo/paymentservice/service/PaymentTimeoutServiceIT.java`
- `payment-service/src/test/java/com/demo/paymentservice/controller/PaymentControllerIT.java`
- `payment-service/src/test/resources/application-test.yml`

### stock-service

**Infrastructure:** No DB. `@SpringBootTest(RANDOM_PORT)` + `@MockitoBean OrderEngineClient`.
Override `simulation.cancellation-delay-seconds` to 0 or 1 in `application-test.yml`.

| Class | What it tests |
|---|---|
| `StockServiceIT` | reserve → in-memory state set; cancel → async `RESERVATION_CANCELLED` sent after delay; ship → `SHIPMENT_REQUESTED` sent |
| `SimulateControllerIT` | Each simulate endpoint fires the correct `StockEvent` via `OrderEngineClient` |

**Files to create:**
- `stock-service/src/test/java/com/demo/stockservice/service/StockServiceIT.java`
- `stock-service/src/test/java/com/demo/stockservice/controller/SimulateControllerIT.java`
- `stock-service/src/test/resources/application-test.yml`

### Maven dependencies to add (root `pom.xml`)

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

Add `testcontainers-bom` to `dependencyManagement`.

**Run:** `mvn test -Dgroups=integration`

---

## Level 3 — API-level E2E Tests

**Tag:** `@Tag("e2e")`

All three services + PostgreSQL run as real Docker containers via `DockerComposeContainer`
(wrapping the existing `docker-compose.yml`). No mocking — real HTTP between services.

### Test Actor Design

Tests use three actors, each representing a real-world participant. Actors expose a
business-readable DSL; HTTP details are hidden inside the actor classes.

| Actor | Represents | HTTP target |
|---|---|---|
| `Client` | End user / GUI | order-engine REST API |
| `PaymentSystem` | External payment provider | payment-service `/simulate/*` |
| `WarehouseSystem` | External warehouse & courier | stock-service `/simulate/*` |

`Client.seesOrderInStatus()` encapsulates Awaitility polling (500ms interval, 10s max).
Callers never deal with async timing.

### DSL Example

```java
@Tag("e2e")
class OrderLifecycleE2ETest {

    static Client          client;
    static PaymentSystem   paymentSystem;
    static WarehouseSystem warehouseSystem;

    @Test
    void customerReceivesOrderAfterSuccessfulPaymentAndShipping() {
        UUID orderId = client.placesOrder("Alice", "MacBook Pro", 1);

        warehouseSystem.confirmsStockAvailable(orderId);
        paymentSystem.receivesPayment(orderId);
        warehouseSystem.shipsOrder(orderId);
        warehouseSystem.confirmsDelivery(orderId);

        client.seesOrderInStatus(orderId, DELIVERED);
    }

    @Test
    void orderIsCancelledWhenPaymentTimesOut() {
        UUID orderId = client.placesOrder("Bob", "iPhone 15", 1);
        warehouseSystem.confirmsStockAvailable(orderId);
        // payment timeout fires automatically (short config via env var)
        client.seesOrderInStatus(orderId, CANCELLED);
    }

    @Test
    void orderIsRefundedWhenStockRunsOut() {
        UUID orderId = client.placesOrder("Charlie", "iPad Pro", 2);
        paymentSystem.receivesPayment(orderId);
        warehouseSystem.reportsSoldOut(orderId);
        client.seesOrderInStatus(orderId, CANCELLED);
    }

    @Test
    void customerCanCancelWhileWaitingForStock() {
        UUID orderId = client.placesOrder("Diana", "MacBook Air", 1);
        warehouseSystem.confirmsStockAvailable(orderId);
        client.cancelsOrder(orderId);
        client.seesOrderInStatus(orderId, CANCELLED);
    }

    @Test
    void customerCanCancelAfterPayment() {
        UUID orderId = client.placesOrder("Eve", "AirPods Pro", 1);
        paymentSystem.receivesPayment(orderId);
        client.cancelsOrder(orderId);
        client.seesOrderInStatus(orderId, CANCELLED);
    }
}
```

### Actor Skeleton

```java
class Client {
    // POST /orders → UUID; DELETE /orders/{id}/cancel;
    // GET /orders/{id} polled by seesOrderInStatus() via Awaitility
    UUID placesOrder(String customer, String product, int qty) { ... }
    void cancelsOrder(UUID orderId) { ... }
    void seesOrderInStatus(UUID orderId, OrderStatus expected) { ... }
}

class PaymentSystem {
    // POST /simulate/payment {orderId, amount: "99.99"}
    void receivesPayment(UUID orderId) { ... }
}

class WarehouseSystem {
    void confirmsStockAvailable(UUID orderId) { /* POST /simulate/stock-available  */ }
    void reportsSoldOut(UUID orderId)          { /* POST /simulate/stock-sold-out   */ }
    void shipsOrder(UUID orderId)              { /* POST /simulate/shipped          */ }
    void confirmsDelivery(UUID orderId)        { /* POST /simulate/delivery-confirmed */ }
}
```

### Scenarios

| Test method | State path | Terminal state |
|---|---|---|
| `customerReceivesOrderAfterSuccessfulPaymentAndShipping` | CREATED→RESERVED→READY_TO_SHIP→SHIPPING→DELIVERED | DELIVERED |
| `orderIsCancelledWhenPaymentTimesOut` | CREATED→RESERVED→RELEASING_RESERVATION→CANCELLED | CANCELLED |
| `orderIsRefundedWhenStockRunsOut` | CREATED→PAID→RETURNING_PAYMENT→CANCELLED | CANCELLED |
| `customerCanCancelWhileWaitingForStock` | CREATED→RESERVED→RELEASING_RESERVATION→CANCELLED | CANCELLED |
| `customerCanCancelAfterPayment` | CREATED→PAID→RETURNING_PAYMENT→CANCELLED | CANCELLED |

> `createOrder` triggers parallel calls to both services so payment and stock events can
> arrive in either order. Scenarios force a specific ordering by calling one actor first;
> Awaitility handles the async gap.

### Infrastructure

- `DockerComposeContainer` (`@BeforeAll`) wrapping `docker-compose.yml`; mapped ports passed to actors
- Short timeouts via env vars: `PAYMENT_TIMEOUT_MINUTES=0`, `SIMULATION_CANCELLATION_DELAY_SECONDS=1`
- `RestAssured` for HTTP inside actors
- `Awaitility` inside `Client.seesOrderInStatus()` only

### Maven dependencies to add (root `pom.xml`)

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### Files to create

- `order-engine/src/test/java/com/demo/orderengine/e2e/OrderLifecycleE2ETest.java`
- `order-engine/src/test/java/com/demo/orderengine/e2e/actors/Client.java`
- `order-engine/src/test/java/com/demo/orderengine/e2e/actors/PaymentSystem.java`
- `order-engine/src/test/java/com/demo/orderengine/e2e/actors/WarehouseSystem.java`
- `order-engine/src/test/java/com/demo/orderengine/e2e/E2ETestSetup.java`

**Run:** `mvn verify -Dgroups=e2e`

---

## Level 4 — Playwright GUI Tests

The API-level E2E tests cover all business logic. The Playwright suite proves only what
requires a real browser:

1. **nginx proxy** — `/api/*` routes correctly strip the prefix and reach order-engine
2. **WebSocket** — STOMP status updates appear on screen without a page refresh

These are smoke tests. Business logic is not re-tested here.

### Scenarios (3 total)

| Test | What it proves |
|---|---|
| `userCanLoginAndSeeShopPage` | SPA served correctly; login flow works |
| `userCanPlaceOrderAndSeeItInMyOrders` | `/api/orders` proxied; order appears in My Orders |
| `orderStatusUpdatesLiveViaWebSocket` | STOMP message updates the status badge without page refresh |

### Implementation Sketch

```typescript
// playwright/tests/order-flow.spec.ts

test('user can place an order and see it in My Orders', async ({ page }) => {
    await page.goto('http://localhost:3000');
    await page.fill('[data-testid="customer-name"]', 'Alice');
    await page.click('[data-testid="login-btn"]');

    await page.click('[data-testid="buy-MacBook Pro"]');
    await page.click('[data-testid="my-orders-nav"]');

    await expect(page.locator('[data-testid="order-status"]')).toHaveText('CREATED');
});

test('order status updates live via WebSocket', async ({ page }) => {
    const orderId = await placeOrderViaApi('Alice', 'MacBook Pro', 1);

    await page.goto('http://localhost:3000/orders?customer=Alice');
    await triggerStockAvailable(orderId);   // call stock-service simulate endpoint

    await expect(page.locator(`[data-testid="status-${orderId}"]`))
        .toHaveText('RESERVED', { timeout: 5000 });
});
```

`placeOrderViaApi` and `triggerStockAvailable` are small TypeScript helpers in
`playwright/helpers/api.ts` that call REST endpoints directly, making test setup faster
than driving everything through the UI.

### Infrastructure

- Target: `http://localhost:3000` (order-ui / nginx, full docker-compose stack running)
- Stack startup: same `docker compose up` used before the Java E2E suite
- Location: `order-ui/playwright/`

### Files to create

- `order-ui/playwright/tests/order-flow.spec.ts`
- `order-ui/playwright/helpers/api.ts`
- `order-ui/playwright.config.ts`
- `order-ui/package.json` — add `@playwright/test` dev dependency + `"test:e2e": "playwright test"` script

### Run

```bash
cd order-ui
npx playwright test             # headless
npx playwright test --headed    # with browser visible
npx playwright show-report      # HTML report
```

---

## Test Execution Summary

| Command | Scope |
|---|---|
| `mvn test -Dgroups=unit` | Unit tests only (fast, no infra) |
| `mvn test -Dgroups=integration` | Integration tests (TestContainers PostgreSQL) |
| `mvn verify -Dgroups=e2e` | Full stack E2E via DockerComposeContainer |
| `cd order-ui && npx playwright test` | GUI smoke tests (stack must be running) |
