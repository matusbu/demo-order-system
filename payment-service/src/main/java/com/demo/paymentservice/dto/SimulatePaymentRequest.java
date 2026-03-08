package com.demo.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record SimulatePaymentRequest(
        @NotNull UUID orderId,
        @NotNull @Positive BigDecimal amount
) {}
