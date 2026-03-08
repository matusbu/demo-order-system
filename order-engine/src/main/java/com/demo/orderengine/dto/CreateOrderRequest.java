package com.demo.orderengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotBlank String customerName,
        @NotBlank String productName,
        @NotNull @Positive Integer quantity
) {}
