package com.demo.stockservice.integration;

import com.demo.stockservice.dto.StockEvent;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.UUID;

public class OrderEngineClient {

    private final RestClient restClient;

    public OrderEngineClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void sendStockEvent(UUID orderId, StockEvent event) {
        restClient.post()
                .uri("/webhook/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new WebhookPayload(orderId, event))
                .retrieve()
                .toBodilessEntity();
    }

    private record WebhookPayload(UUID orderId, StockEvent event) {}
}
