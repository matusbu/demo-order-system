package com.demo.paymentservice.integration;

import com.demo.paymentservice.dto.PaymentEvent;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.UUID;

public class OrderEngineClient {

    private final RestClient restClient;

    public OrderEngineClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void sendPaymentEvent(UUID orderId, PaymentEvent event) {
        restClient.post()
                .uri("/webhook/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new WebhookPayload(orderId, event))
                .retrieve()
                .toBodilessEntity();
    }

    private record WebhookPayload(UUID orderId, PaymentEvent event) {}
}
