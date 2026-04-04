package com.demo.stockservice.controller;

import com.demo.stockservice.dto.StockEvent;
import com.demo.stockservice.integration.OrderEngineClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SimulateControllerIT {

    @MockitoBean
    private OrderEngineClient orderEngineClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void stockAvailable_returns200_andSendsStockReservedEvent() {
        UUID orderId = UUID.randomUUID();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/simulate/stock-available", Map.of("orderId", orderId), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.STOCK_RESERVED);
    }

    @Test
    void stockSoldOut_returns200_andSendsStockSoldOutEvent() {
        UUID orderId = UUID.randomUUID();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/simulate/stock-sold-out", Map.of("orderId", orderId), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.STOCK_SOLD_OUT);
    }

    @Test
    void shipped_returns200_andSendsShipmentRequestedEvent() {
        UUID orderId = UUID.randomUUID();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/simulate/shipped", Map.of("orderId", orderId), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.SHIPMENT_REQUESTED);
    }

    @Test
    void deliveryConfirmed_returns200_andSendsDeliveryConfirmedEvent() {
        UUID orderId = UUID.randomUUID();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/simulate/delivery-confirmed", Map.of("orderId", orderId), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(orderEngineClient).sendStockEvent(orderId, StockEvent.DELIVERY_CONFIRMED);
    }

    @Test
    void stockAvailable_returns400_whenOrderIdMissing() {
        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                "/simulate/stock-available", Map.of(), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
