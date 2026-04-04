package com.demo.orderengine.statemachine;

import com.demo.orderengine.domain.OrderEvent;
import com.demo.orderengine.domain.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Map<OrderEvent, OrderStatus>> TRANSITIONS;

    static {
        TRANSITIONS = new EnumMap<>(OrderStatus.class);

        TRANSITIONS.put(OrderStatus.CREATED, eventsOf(
                OrderEvent.STOCK_RESERVED,          OrderStatus.RESERVED,
                OrderEvent.STOCK_SOLD_OUT,          OrderStatus.CANCELLED,
                OrderEvent.PAYMENT_RECEIVED,        OrderStatus.PAID,
                OrderEvent.PAYMENT_TIMEOUT,         OrderStatus.CANCELLED,
                OrderEvent.USER_CANCELLED,          OrderStatus.CANCELLED
        ));

        TRANSITIONS.put(OrderStatus.RESERVED, eventsOf(
                OrderEvent.PAYMENT_RECEIVED,        OrderStatus.READY_TO_SHIP,
                OrderEvent.PAYMENT_TIMEOUT,         OrderStatus.RELEASING_RESERVATION,
                OrderEvent.USER_CANCELLED,          OrderStatus.RELEASING_RESERVATION
        ));

        TRANSITIONS.put(OrderStatus.RELEASING_RESERVATION, eventsOf(
                OrderEvent.RESERVATION_CANCELLED,   OrderStatus.CANCELLED
        ));

        TRANSITIONS.put(OrderStatus.PAID, eventsOf(
                OrderEvent.STOCK_RESERVED,          OrderStatus.READY_TO_SHIP,
                OrderEvent.STOCK_SOLD_OUT,          OrderStatus.RETURNING_PAYMENT,
                OrderEvent.USER_CANCELLED,          OrderStatus.RETURNING_PAYMENT
        ));

        TRANSITIONS.put(OrderStatus.RETURNING_PAYMENT, eventsOf(
                OrderEvent.PAYMENT_RETURNED,        OrderStatus.CANCELLED
        ));

        TRANSITIONS.put(OrderStatus.READY_TO_SHIP, eventsOf(
                OrderEvent.SHIPMENT_REQUESTED,      OrderStatus.SHIPPING
        ));

        TRANSITIONS.put(OrderStatus.SHIPPING, eventsOf(
                OrderEvent.DELIVERY_CONFIRMED,      OrderStatus.DELIVERED
        ));
    }

    public OrderStatus transition(OrderStatus current, OrderEvent event) {
        Map<OrderEvent, OrderStatus> validEvents = TRANSITIONS.get(current);
        if (validEvents == null || !validEvents.containsKey(event)) {
            throw new IllegalStateTransitionException(current, event);
        }
        return validEvents.get(event);
    }

    private static Map<OrderEvent, OrderStatus> eventsOf(Object... pairs) {
        Map<OrderEvent, OrderStatus> map = new EnumMap<>(OrderEvent.class);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((OrderEvent) pairs[i], (OrderStatus) pairs[i + 1]);
        }
        return map;
    }
}
