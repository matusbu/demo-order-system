# demo-order-system

A demo e-commerce order management system built for educational purposes — used to illustrate Test Automation concepts, testing strategies, and software design patterns.

## Services

| Service           | Port | Description                                      |
|-------------------|------|--------------------------------------------------|
| `order-engine`    | 8080 | Manages order lifecycle and persists to Postgres |
| `payment-service` | 8081 | Stateless payment processing service             |

## Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose

## Getting Started

### 1. Start the database

```bash
docker-compose up -d
```

This starts a PostgreSQL 16 instance on port `5432` with:
- Database: `order_db`
- Username: `order_user`
- Password: `order_pass`

### 2. Start order-engine

```bash
cd order-engine
mvn spring-boot:run
```

### 3. Start payment-service

```bash
cd payment-service
mvn spring-boot:run
```

### Stop everything

```bash
docker-compose down
```
