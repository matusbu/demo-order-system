package com.demo.e2e.actor;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Represents the warehouse system that processes stock reservations and shipments.
 */
public class Warehouse {

    private final String stockServiceBaseUrl;

    public Warehouse(String stockServiceBaseUrl) {
        this.stockServiceBaseUrl = stockServiceBaseUrl;
    }

    public void confirmsStockIsAvailable(UUID orderId) {
        given()
                .baseUri(stockServiceBaseUrl)
                .contentType("application/json")
                .body(Map.of("orderId", orderId))
                .post("/simulate/stock-available")
                .then()
                .statusCode(200);
    }

    public void confirmsStockIsSoldOut(UUID orderId) {
        given()
                .baseUri(stockServiceBaseUrl)
                .contentType("application/json")
                .body(Map.of("orderId", orderId))
                .post("/simulate/stock-sold-out")
                .then()
                .statusCode(200);
    }

    public void confirmsOrderHasBeenShipped(UUID orderId) {
        given()
                .baseUri(stockServiceBaseUrl)
                .contentType("application/json")
                .body(Map.of("orderId", orderId))
                .post("/simulate/shipped")
                .then()
                .statusCode(200);
    }

    public void confirmsDelivery(UUID orderId) {
        given()
                .baseUri(stockServiceBaseUrl)
                .contentType("application/json")
                .body(Map.of("orderId", orderId))
                .post("/simulate/delivery-confirmed")
                .then()
                .statusCode(200);
    }
}
