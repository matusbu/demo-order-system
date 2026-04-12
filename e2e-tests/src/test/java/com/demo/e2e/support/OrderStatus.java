package com.demo.e2e.support;

/** Local mirror of order-engine's OrderStatus — kept in sync by convention. */
public enum OrderStatus {
    CREATED,
    RESERVED,
    PAID,
    READY_TO_SHIP,
    SHIPPING,
    DELIVERED,
    RELEASING_RESERVATION,
    RETURNING_PAYMENT,
    CANCELLED
}
