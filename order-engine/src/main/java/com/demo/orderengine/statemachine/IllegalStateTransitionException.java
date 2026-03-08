package com.demo.orderengine.statemachine;

import com.demo.orderengine.domain.OrderEvent;
import com.demo.orderengine.domain.OrderStatus;

public class IllegalStateTransitionException extends RuntimeException {

    public IllegalStateTransitionException(OrderStatus current, OrderEvent event) {
        super("Invalid transition: event [" + event + "] is not allowed in state [" + current + "]");
    }
}
