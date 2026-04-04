package com.demo.orderengine.statemachine;

import com.demo.orderengine.domain.OrderEvent;
import com.demo.orderengine.domain.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(OrderStatus.CREATED,               OrderEvent.STOCK_RESERVED,        OrderStatus.RESERVED),
                Arguments.of(OrderStatus.CREATED,               OrderEvent.STOCK_SOLD_OUT,         OrderStatus.CANCELLED),
                Arguments.of(OrderStatus.CREATED,               OrderEvent.PAYMENT_RECEIVED,       OrderStatus.PAID),
                Arguments.of(OrderStatus.CREATED,               OrderEvent.PAYMENT_TIMEOUT,        OrderStatus.CANCELLED),
                Arguments.of(OrderStatus.CREATED,               OrderEvent.USER_CANCELLED,         OrderStatus.CANCELLED),
                Arguments.of(OrderStatus.RESERVED,              OrderEvent.PAYMENT_RECEIVED,       OrderStatus.READY_TO_SHIP),
                Arguments.of(OrderStatus.RESERVED,              OrderEvent.PAYMENT_TIMEOUT,        OrderStatus.RELEASING_RESERVATION),
                Arguments.of(OrderStatus.RESERVED,              OrderEvent.USER_CANCELLED,         OrderStatus.RELEASING_RESERVATION),
                Arguments.of(OrderStatus.RELEASING_RESERVATION, OrderEvent.RESERVATION_CANCELLED,  OrderStatus.CANCELLED),
                Arguments.of(OrderStatus.PAID,                  OrderEvent.STOCK_RESERVED,         OrderStatus.READY_TO_SHIP),
                Arguments.of(OrderStatus.PAID,                  OrderEvent.STOCK_SOLD_OUT,         OrderStatus.RETURNING_PAYMENT),
                Arguments.of(OrderStatus.PAID,                  OrderEvent.USER_CANCELLED,         OrderStatus.RETURNING_PAYMENT),
                Arguments.of(OrderStatus.RETURNING_PAYMENT,     OrderEvent.PAYMENT_RETURNED,       OrderStatus.CANCELLED),
                Arguments.of(OrderStatus.READY_TO_SHIP,         OrderEvent.SHIPMENT_REQUESTED,     OrderStatus.SHIPPING),
                Arguments.of(OrderStatus.SHIPPING,              OrderEvent.DELIVERY_CONFIRMED,     OrderStatus.DELIVERED)
        );
    }

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @MethodSource("validTransitions")
    void shouldTransitionToExpectedStatus(OrderStatus from, OrderEvent event, OrderStatus expectedTo) {
        OrderStatus result = stateMachine.transition(from, event);
        assertThat(result).isEqualTo(expectedTo);
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                // Terminal states accept no events
                Arguments.of(OrderStatus.DELIVERED,             OrderEvent.SHIPMENT_REQUESTED),
                Arguments.of(OrderStatus.DELIVERED,             OrderEvent.DELIVERY_CONFIRMED),
                Arguments.of(OrderStatus.CANCELLED,             OrderEvent.USER_CANCELLED),
                Arguments.of(OrderStatus.CANCELLED,             OrderEvent.PAYMENT_RETURNED),
                // Events not valid for a given state
                Arguments.of(OrderStatus.CREATED,               OrderEvent.DELIVERY_CONFIRMED),
                Arguments.of(OrderStatus.RESERVED,              OrderEvent.STOCK_SOLD_OUT),
                Arguments.of(OrderStatus.RESERVED,              OrderEvent.DELIVERY_CONFIRMED),
                Arguments.of(OrderStatus.RELEASING_RESERVATION, OrderEvent.PAYMENT_RECEIVED),
                Arguments.of(OrderStatus.PAID,                  OrderEvent.PAYMENT_TIMEOUT),
                Arguments.of(OrderStatus.RETURNING_PAYMENT,     OrderEvent.STOCK_RESERVED),
                Arguments.of(OrderStatus.READY_TO_SHIP,         OrderEvent.PAYMENT_RECEIVED),
                Arguments.of(OrderStatus.SHIPPING,              OrderEvent.STOCK_RESERVED)
        );
    }

    @ParameterizedTest(name = "{0} + {1} → IllegalStateTransitionException")
    @MethodSource("invalidTransitions")
    void shouldThrowForInvalidTransition(OrderStatus from, OrderEvent event) {
        assertThatThrownBy(() -> stateMachine.transition(from, event))
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(event.name());
    }
}
