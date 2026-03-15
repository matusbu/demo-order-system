package com.demo.orderengine.integration;

import com.demo.orderengine.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Sends fire-and-forget HTTP notifications to payment-service and stock-service.
 *
 * NOTE: calls are made within the calling transaction. A production implementation
 * should use the transactional outbox pattern to decouple DB commits from HTTP calls.
 */
public class IntegrationClient {

    private static final Logger log = LoggerFactory.getLogger(IntegrationClient.class);

    private final RestClient paymentClient;
    private final RestClient stockClient;

    public IntegrationClient(RestClient paymentClient, RestClient stockClient) {
        this.paymentClient = paymentClient;
        this.stockClient = stockClient;
    }

    public void notifyOrderCreated(Order order) {
        log.info("Calling payment-service [POST /orders] for order {}", order.getId());
        try {
            paymentClient.post()
                    .uri("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OrderPayload(order.getId(), order.getCustomerName(), order.getProductName(), order.getQuantity()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Call to payment-service failed for order {}: {}", order.getId(), e.getMessage());
            throw e;
        }
    }

    public void notifyReserveStock(Order order) {
        log.info("Calling stock-service [POST /orders/reserve] for order {}", order.getId());
        try {
            stockClient.post()
                    .uri("/orders/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StockPayload(order.getId(), order.getProductName(), order.getQuantity()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Call to stock-service failed for order {}: {}", order.getId(), e.getMessage());
            throw e;
        }
    }

    public void notifyCancelReservation(Order order) {
        log.info("Calling stock-service [POST /orders/{}/cancel-reservation] for order {}", order.getId(), order.getId());
        try {
            stockClient.post()
                    .uri("/orders/{orderId}/cancel-reservation", order.getId())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Call to stock-service failed for order {}: {}", order.getId(), e.getMessage());
            throw e;
        }
    }

    public void notifyReturnPayment(Order order) {
        log.info("Calling payment-service [POST /orders/{}/return] for order {}", order.getId(), order.getId());
        try {
            paymentClient.post()
                    .uri("/orders/{orderId}/return", order.getId())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Call to payment-service failed for order {}: {}", order.getId(), e.getMessage());
            throw e;
        }
    }

    public void notifyShipOrder(Order order) {
        log.info("Calling stock-service [POST /orders/{}/ship] for order {}", order.getId(), order.getId());
        try {
            stockClient.post()
                    .uri("/orders/{orderId}/ship", order.getId())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Call to stock-service failed for order {}: {}", order.getId(), e.getMessage());
            throw e;
        }
    }

    private record OrderPayload(UUID orderId, String customerName, String productName, Integer quantity) {}
    private record StockPayload(UUID orderId, String productName, Integer quantity) {}
    private record OrderIdPayload(UUID orderId) {}
}
