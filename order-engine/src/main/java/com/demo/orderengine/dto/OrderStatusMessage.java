package com.demo.orderengine.dto;

import com.demo.orderengine.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderStatusMessage(UUID orderId, OrderStatus status, LocalDateTime updatedAt) {}
