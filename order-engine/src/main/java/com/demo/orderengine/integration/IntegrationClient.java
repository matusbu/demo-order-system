package com.demo.orderengine.integration;

import com.demo.orderengine.domain.Order;
import com.demo.stockservice.grpc.OrderIdRequest;
import com.demo.stockservice.grpc.ReserveRequest;
import com.demo.stockservice.grpc.StockServiceGrpc;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Sends fire-and-forget notifications to payment-service (REST) and stock-service (gRPC).
 *
 * NOTE: calls are made within the calling transaction. A production implementation
 * should use the transactional outbox pattern to decouple DB commits from these calls.
 */
public class IntegrationClient {

    private static final Logger log = LoggerFactory.getLogger(IntegrationClient.class);

    private final RestClient paymentClient;
    private final StockServiceGrpc.StockServiceBlockingStub stockServiceStub;

    public IntegrationClient(RestClient paymentClient, StockServiceGrpc.StockServiceBlockingStub stockServiceStub) {
        this.paymentClient = paymentClient;
        this.stockServiceStub = stockServiceStub;
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
        log.info("Calling stock-service [gRPC Reserve] for order {}", order.getId());
        try {
            stockServiceStub.reserve(ReserveRequest.newBuilder()
                    .setOrderId(order.getId().toString())
                    .setProductName(order.getProductName())
                    .setQuantity(order.getQuantity())
                    .build());
        } catch (StatusRuntimeException e) {
            log.error("Call to stock-service failed for order {}: {}", order.getId(), e.getMessage());
            throw e;
        }
    }

    public void notifyCancelReservation(Order order) {
        log.info("Calling stock-service [gRPC CancelReservation] for order {}", order.getId());
        try {
            stockServiceStub.cancelReservation(OrderIdRequest.newBuilder()
                    .setOrderId(order.getId().toString())
                    .build());
        } catch (StatusRuntimeException e) {
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
        log.info("Calling stock-service [gRPC Ship] for order {}", order.getId());
        try {
            stockServiceStub.ship(OrderIdRequest.newBuilder()
                    .setOrderId(order.getId().toString())
                    .build());
        } catch (StatusRuntimeException e) {
            log.error("Call to stock-service failed for order {}: {}", order.getId(), e.getMessage());
            throw e;
        }
    }

    private record OrderPayload(UUID orderId, String customerName, String productName, Integer quantity) {}
}
