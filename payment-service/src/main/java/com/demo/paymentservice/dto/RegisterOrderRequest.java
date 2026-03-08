package com.demo.paymentservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterOrderRequest(@NotNull UUID orderId) {}
