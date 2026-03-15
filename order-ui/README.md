# order-ui

React + TypeScript + Vite frontend for the `demo-order-system`.

## Pages

| Page      | Description                                                                 |
|-----------|-----------------------------------------------------------------------------|
| Login     | Enter a customer name to start a session (stored in `localStorage`)         |
| Shop      | Browse 5 Apple products and place orders via the order-engine REST API      |
| My Orders | View your order history with live status updates via STOMP/SockJS WebSocket |

## Communication

- **REST** — axios; all requests go to `/api/*`, which nginx proxies to `order-engine` (port 8080)
- **WebSocket** — STOMP over SockJS; subscribes to `/topic/orders/{customerName}` on the order-engine WebSocket endpoint (`/ws/orders`)

## Running

### Preferred — Docker Compose

```bash
docker-compose up -d --build
```

The UI is served by nginx on **http://localhost:3000**.

### Local development

```bash
cd order-ui
npm install
npm run dev
```

The Vite dev server starts on **http://localhost:5173** and proxies `/api/*` to `http://localhost:8080`.

## Environment variable

| Variable              | Default                   | Description                          |
|-----------------------|---------------------------|--------------------------------------|
| `VITE_ORDER_ENGINE_URL` | `` (empty — uses nginx proxy) | Override the order-engine base URL (e.g. `http://localhost:8080` for local dev without nginx) |
