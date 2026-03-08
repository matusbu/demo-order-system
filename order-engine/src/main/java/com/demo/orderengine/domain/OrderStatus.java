package com.demo.orderengine.domain;

public enum OrderStatus {
    CREATED,
    RESERVED,
    PAID,
    RELEASING_RESERVATION,
    RETURNING_PAYMENT,
    READY_TO_SHIP,
    SHIPPING,
    DELIVERED,
    CANCELLED
}
