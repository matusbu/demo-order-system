package com.demo.orderengine.dto;

import com.demo.orderengine.domain.OrderEvent;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockWebhookRequest(
        @NotNull UUID orderId,
        @NotNull OrderEvent event
) {}
