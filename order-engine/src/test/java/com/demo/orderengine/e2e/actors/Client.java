package com.demo.orderengine.e2e.actors;

import com.demo.orderengine.domain.OrderStatus;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the end user interacting with the system through the GUI.
 * All calls target the order-engine REST API.
 */
public class Client {

    private final String baseUrl;

    public Client(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public UUID placesOrder(String customerName, String productName, int quantity) {
        String id = RestAssured.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of("customerName", customerName, "productName", productName, "quantity", quantity))
                .post("/orders")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        return UUID.fromString(id);
    }

    public void cancelsOrder(UUID orderId) {
        RestAssured.given()
                .baseUri(baseUrl)
                .delete("/orders/{id}/cancel", orderId)
                .then()
                .statusCode(200);
    }

    /**
     * Polls the order status until it matches the expected value or the 15-second timeout expires.
     * Use as both a synchronisation point between test steps and a final assertion.
     */
    public void seesOrderInStatus(UUID orderId, OrderStatus expected) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() ->
                        RestAssured.given()
                                .baseUri(baseUrl)
                                .get("/orders/{id}", orderId)
                                .then()
                                .statusCode(200)
                                .body("status", Matchers.equalTo(expected.name()))
                );
    }
}
