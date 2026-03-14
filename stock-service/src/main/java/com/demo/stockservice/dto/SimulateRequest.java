package com.demo.stockservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SimulateRequest(@NotNull UUID orderId) {}
