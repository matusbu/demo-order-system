package com.demo.e2e.actor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Represents the external payment system that processes payments for orders.
 */
public class PaymentSystem {

    private final String paymentServiceBaseUrl;

    public PaymentSystem(String paymentServiceBaseUrl) {
        this.paymentServiceBaseUrl = paymentServiceBaseUrl;
    }

    public void receivesPayment(UUID orderId, BigDecimal amount) {
        given()
                .baseUri(paymentServiceBaseUrl)
                .contentType("application/json")
                .body(Map.of("orderId", orderId, "amount", amount))
                .post("/simulate/payment")
                .then()
                .statusCode(200);
    }
}
