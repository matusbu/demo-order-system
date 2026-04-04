package com.demo.orderengine.e2e.actors;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.util.Map;
import java.util.UUID;

/**
 * Represents the external warehouse and courier system.
 * Controls stock-service via its simulation endpoints.
 */
public class WarehouseSystem {

    private final String baseUrl;

    public WarehouseSystem(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void confirmsStockAvailable(UUID orderId) {
        simulate("/simulate/stock-available", orderId);
    }

    public void reportsSoldOut(UUID orderId) {
        simulate("/simulate/stock-sold-out", orderId);
    }

    public void shipsOrder(UUID orderId) {
        simulate("/simulate/shipped", orderId);
    }

    public void confirmsDelivery(UUID orderId) {
        simulate("/simulate/delivery-confirmed", orderId);
    }

    private void simulate(String path, UUID orderId) {
        RestAssured.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of("orderId", orderId.toString()))
                .post(path)
                .then()
                .statusCode(200);
    }
}
