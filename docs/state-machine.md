# Order State Machine

## Valid Transitions

| From                   | Event                  | To                     |
|------------------------|------------------------|------------------------|
| CREATED                | STOCK_RESERVED         | RESERVED               |
| CREATED                | STOCK_SOLD_OUT         | CANCELLED              |
| CREATED                | PAYMENT_RECEIVED       | PAID                   |
| CREATED                | USER_CANCELLED         | CANCELLED              |
| RESERVED               | PAYMENT_RECEIVED       | READY_TO_SHIP          |
| RESERVED               | PAYMENT_TIMEOUT        | RELEASING_RESERVATION  |
| RESERVED               | USER_CANCELLED         | RELEASING_RESERVATION  |
| RELEASING_RESERVATION  | RESERVATION_CANCELLED  | CANCELLED              |
| PAID                   | STOCK_RESERVED         | READY_TO_SHIP          |
| PAID                   | STOCK_SOLD_OUT         | RETURNING_PAYMENT      |
| PAID                   | USER_CANCELLED         | RETURNING_PAYMENT      |
| RETURNING_PAYMENT      | PAYMENT_RETURNED       | CANCELLED              |
| READY_TO_SHIP          | SHIPMENT_REQUESTED     | SHIPPING               |
| SHIPPING               | DELIVERY_CONFIRMED     | DELIVERED              |

## Terminal States

### DELIVERED
The order has been successfully fulfilled. The shipment was confirmed as received
by the customer. No further transitions are possible.

Reached via: `CREATED → RESERVED → READY_TO_SHIP → SHIPPING → DELIVERED`
or `CREATED → PAID → READY_TO_SHIP → SHIPPING → DELIVERED`

### CANCELLED
The order was terminated without fulfilment. Multiple paths lead here, depending
on where in the lifecycle the failure or cancellation occurred:

- Stock was never available (`STOCK_SOLD_OUT` from `CREATED`)
- User cancelled before payment or stock was confirmed
- Compensation flows completed (see below)

## Compensation Flows

### RELEASING_RESERVATION
**Triggered when:** Stock was reserved for an order, but payment never arrived
(timeout) or the user cancelled after the reservation was made.

**Flow:** `RESERVED → RELEASING_RESERVATION → CANCELLED`

The reserved stock must be released back to inventory before the order can be
marked as cancelled. The `RESERVATION_CANCELLED` event confirms the release.

### RETURNING_PAYMENT
**Triggered when:** Payment was received upfront, but stock could not be fulfilled
(`STOCK_SOLD_OUT`) or the user cancelled after payment was already collected.

**Flow:** `PAID → RETURNING_PAYMENT → CANCELLED`

The collected payment must be refunded to the customer before the order can be
marked as cancelled. The `PAYMENT_RETURNED` event confirms the refund.
