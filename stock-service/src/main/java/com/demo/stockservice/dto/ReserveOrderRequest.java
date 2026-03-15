package com.demo.stockservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ReserveOrderRequest(
        @NotNull UUID orderId,
        @NotBlank String productName,
        @NotNull @Positive Integer quantity
) {}
