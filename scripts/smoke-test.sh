#!/usr/bin/env bash
set -euo pipefail

ORDER_ENGINE="http://localhost:8080"
PAYMENT_SERVICE="http://localhost:8081"
STOCK_SERVICE="http://localhost:8082"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

pass() { echo -e "${GREEN}✔ $1${NC}"; }
fail() { echo -e "${RED}✘ $1${NC}"; exit 1; }

assert_status() {
  local actual="$1"
  local expected="$2"
  local step="$3"
  if [ "$actual" = "CANCELLED" ]; then
    fail "$step — order was CANCELLED unexpectedly"
  elif [ "$actual" = "$expected" ]; then
    pass "$step — status: $actual"
  else
    fail "$step — expected $expected, got $actual"
  fi
}

echo "=== Smoke Test: Happy Path ==="
echo ""

# ── Step 1: Create order ───────────────────────────────────────────────────────
echo "── Step 1: POST /orders → create order"
RESPONSE=$(curl -sf -X POST "$ORDER_ENGINE/orders" \
  -H "Content-Type: application/json" \
  -d '{"customerName": "Alice", "productName": "Widget", "quantity": 2}')
echo "$RESPONSE" | jq .

ORDER_ID=$(echo "$RESPONSE" | jq -r '.id')
STATUS=$(echo "$RESPONSE" | jq -r '.status')
assert_status "$STATUS" "CREATED" "Order created"
echo ""

# ── Step 2: Verify initial status ─────────────────────────────────────────────
echo "── Step 2: GET /orders/$ORDER_ID → verify CREATED"
RESPONSE=$(curl -sf "$ORDER_ENGINE/orders/$ORDER_ID")
echo "$RESPONSE" | jq .
STATUS=$(echo "$RESPONSE" | jq -r '.status')
assert_status "$STATUS" "CREATED" "Initial status"
echo ""

# ── Step 3: Simulate stock available ──────────────────────────────────────────
echo "── Step 3: POST /simulate/stock-available → stock-service"
curl -sf -X POST "$STOCK_SERVICE/simulate/stock-available" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\": \"$ORDER_ID\"}"
echo "(no response body)"
echo ""

# ── Step 4: Verify RESERVED ───────────────────────────────────────────────────
echo "── Step 4: GET /orders/$ORDER_ID → verify RESERVED"
RESPONSE=$(curl -sf "$ORDER_ENGINE/orders/$ORDER_ID")
echo "$RESPONSE" | jq .
STATUS=$(echo "$RESPONSE" | jq -r '.status')
assert_status "$STATUS" "RESERVED" "Stock reserved"
echo ""

# ── Step 5: Simulate payment ──────────────────────────────────────────────────
echo "── Step 5: POST /simulate/payment → payment-service"
curl -sf -X POST "$PAYMENT_SERVICE/simulate/payment" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\": \"$ORDER_ID\", \"amount\": 49.99}"
echo "(no response body)"
echo ""

# ── Step 6: Verify READY_TO_SHIP ──────────────────────────────────────────────
echo "── Step 6: GET /orders/$ORDER_ID → verify READY_TO_SHIP"
RESPONSE=$(curl -sf "$ORDER_ENGINE/orders/$ORDER_ID")
echo "$RESPONSE" | jq .
STATUS=$(echo "$RESPONSE" | jq -r '.status')
assert_status "$STATUS" "READY_TO_SHIP" "Payment received"
echo ""

# ── Step 7: Simulate shipped ──────────────────────────────────────────────────
echo "── Step 7: POST /simulate/shipped → stock-service"
curl -sf -X POST "$STOCK_SERVICE/simulate/shipped" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\": \"$ORDER_ID\"}"
echo "(no response body)"
echo ""

# ── Step 8: Verify SHIPPING ───────────────────────────────────────────────────
echo "── Step 8: GET /orders/$ORDER_ID → verify SHIPPING"
RESPONSE=$(curl -sf "$ORDER_ENGINE/orders/$ORDER_ID")
echo "$RESPONSE" | jq .
STATUS=$(echo "$RESPONSE" | jq -r '.status')
assert_status "$STATUS" "SHIPPING" "Shipment requested"
echo ""

# ── Step 9: Simulate delivery confirmed ───────────────────────────────────────
echo "── Step 9: POST /simulate/delivery-confirmed → stock-service"
curl -sf -X POST "$STOCK_SERVICE/simulate/delivery-confirmed" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\": \"$ORDER_ID\"}"
echo "(no response body)"
echo ""

# ── Step 10: Verify DELIVERED ─────────────────────────────────────────────────
echo "── Step 10: GET /orders/$ORDER_ID → verify DELIVERED"
RESPONSE=$(curl -sf "$ORDER_ENGINE/orders/$ORDER_ID")
echo "$RESPONSE" | jq .
STATUS=$(echo "$RESPONSE" | jq -r '.status')
assert_status "$STATUS" "DELIVERED" "Final order status"
echo ""

# ── Step 11: List orders by customer ──────────────────────────────────────────
echo "── Step 11: GET /orders?customerName=Alice → list orders for customer"
RESPONSE=$(curl -sf "$ORDER_ENGINE/orders?customerName=Alice")
echo "$RESPONSE" | jq .
COUNT=$(echo "$RESPONSE" | jq 'length')
if [ "$COUNT" -gt 0 ]; then
  pass "GET /orders?customerName=Alice — returned $COUNT order(s)"
else
  fail "GET /orders?customerName=Alice — expected at least 1 order, got 0"
fi
echo ""

echo "=== All steps passed ==="
