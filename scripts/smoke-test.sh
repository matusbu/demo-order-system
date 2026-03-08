#!/usr/bin/env bash
set -euo pipefail

ORDER_ENGINE="http://localhost:8080"
PAYMENT_SERVICE="http://localhost:8081"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

pass() { echo -e "${GREEN}✔ $1${NC}"; }
fail() { echo -e "${RED}✘ $1${NC}"; exit 1; }

assert_status() {
  local actual="$1"
  local expected="$2"
  local step="$3"
  if [ "$actual" = "$expected" ]; then
    pass "$step — status: $actual"
  else
    fail "$step — expected $expected, got $actual"
  fi
}

echo "=== Smoke Test: Happy Path ==="
echo ""

# ── Step 1: Create order ───────────────────────────────────────────────────────
echo "── Step 1: Create order"
RESPONSE=$(curl -sf -X POST "$ORDER_ENGINE/orders" \
  -H "Content-Type: application/json" \
  -d '{"customerName": "Alice", "productName": "Widget", "quantity": 2}')
echo "$RESPONSE" | jq .

ORDER_ID=$(echo "$RESPONSE" | jq -r '.id')
STATUS=$(echo "$RESPONSE" | jq -r '.status')
assert_status "$STATUS" "CREATED" "Order created"
echo ""

# ── Step 2: Simulate payment (payment-service → triggers PAYMENT_RECEIVED) ────
echo "── Step 2: Simulate payment"
RESPONSE=$(curl -sf -X POST "$PAYMENT_SERVICE/simulate/payment" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\": \"$ORDER_ID\", \"amount\": 49.99}")
echo "(no response body)"

# Give payment-service time to call back order-engine
sleep 1

RESPONSE=$(curl -sf "$ORDER_ENGINE/orders/$ORDER_ID")
echo "$RESPONSE" | jq .
STATUS=$(echo "$RESPONSE" | jq -r '.status')
assert_status "$STATUS" "PAID" "Payment received"
echo ""

# ── Step 3: Stock reserved ─────────────────────────────────────────────────────
echo "── Step 3: Stock reserved"
RESPONSE=$(curl -sf -X POST "$ORDER_ENGINE/webhook/stock" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\": \"$ORDER_ID\", \"event\": \"STOCK_RESERVED\"}")
echo "$RESPONSE" | jq .
STATUS=$(echo "$RESPONSE" | jq -r '.status')
assert_status "$STATUS" "READY_TO_SHIP" "Stock reserved"
echo ""

# ── Step 4: Shipment requested ────────────────────────────────────────────────
echo "── Step 4: Shipment requested"
RESPONSE=$(curl -sf -X POST "$ORDER_ENGINE/webhook/stock" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\": \"$ORDER_ID\", \"event\": \"SHIPMENT_REQUESTED\"}")
echo "$RESPONSE" | jq .
STATUS=$(echo "$RESPONSE" | jq -r '.status')
assert_status "$STATUS" "SHIPPING" "Shipment requested"
echo ""

# ── Step 5: Delivery confirmed ────────────────────────────────────────────────
echo "── Step 5: Delivery confirmed"
RESPONSE=$(curl -sf -X POST "$ORDER_ENGINE/webhook/stock" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\": \"$ORDER_ID\", \"event\": \"DELIVERY_CONFIRMED\"}")
echo "$RESPONSE" | jq .
STATUS=$(echo "$RESPONSE" | jq -r '.status')
assert_status "$STATUS" "DELIVERED" "Delivery confirmed"
echo ""

# ── Step 6: Final state assertion ─────────────────────────────────────────────
echo "── Step 6: Final GET /orders/$ORDER_ID"
RESPONSE=$(curl -sf "$ORDER_ENGINE/orders/$ORDER_ID")
echo "$RESPONSE" | jq .
STATUS=$(echo "$RESPONSE" | jq -r '.status')
assert_status "$STATUS" "DELIVERED" "Final order status"
echo ""

echo "=== All steps passed ==="
