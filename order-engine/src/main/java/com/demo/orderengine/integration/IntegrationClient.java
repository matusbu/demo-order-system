package com.demo.orderengine.integration;

import com.demo.orderengine.domain.Order;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Sends fire-and-forget HTTP notifications to payment-service and stock-service.
 *
 * NOTE: calls are made within the calling transaction. A production implementation
 * should use the transactional outbox pattern to decouple DB commits from HTTP calls.
 */
public class IntegrationClient {

    private final RestClient paymentClient;
    private final RestClient stockClient;

    public IntegrationClient(RestClient paymentClient, RestClient stockClient) {
        this.paymentClient = paymentClient;
        this.stockClient = stockClient;
    }

    public void notifyOrderCreated(Order order) {
        paymentClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new OrderPayload(order.getId(), order.getCustomerName(), order.getProductName(), order.getQuantity()))
                .retrieve()
                .toBodilessEntity();
    }

    public void notifyReserveStock(Order order) {
        // TODO: stock-service not implemented yet
//        stockClient.post()
//                .uri("/notifications/reserve-stock")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(new StockPayload(order.getId(), order.getProductName(), order.getQuantity()))
//                .retrieve()
//                .toBodilessEntity();
    }

    public void notifyCancelReservation(Order order) {
        // TODO: stock-service not implemented yet
//        stockClient.post()
//                .uri("/notifications/cancel-reservation")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(new OrderIdPayload(order.getId()))
//                .retrieve()
//                .toBodilessEntity();
    }

    public void notifyReturnPayment(Order order) {
        paymentClient.post()
                .uri("/orders/{orderId}/return", order.getId())
                .retrieve()
                .toBodilessEntity();
    }

    public void notifyShipOrder(Order order) {
        // TODO: stock-service not implemented yet
//        stockClient.post()
//                .uri("/notifications/ship-order")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(new OrderIdPayload(order.getId()))
//                .retrieve()
//                .toBodilessEntity();
    }

    private record OrderPayload(UUID orderId, String customerName, String productName, Integer quantity) {}
    private record StockPayload(UUID orderId, String productName, Integer quantity) {}
    private record OrderIdPayload(UUID orderId) {}
}
