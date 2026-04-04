package com.demo.orderengine.e2e.actors;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.util.Map;
import java.util.UUID;

/**
 * Represents the external payment provider.
 * Controls payment-service via its simulation endpoints.
 */
public class PaymentSystem {

    private final String baseUrl;

    public PaymentSystem(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void receivesPayment(UUID orderId) {
        RestAssured.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of("orderId", orderId.toString(), "amount", "99.99"))
                .post("/simulate/payment")
                .then()
                .statusCode(200);
    }

    public void timesOutPayment(UUID orderId) {
        RestAssured.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of("orderId", orderId.toString()))
                .post("/simulate/payment-timeout")
                .then()
                .statusCode(200);
    }
}
