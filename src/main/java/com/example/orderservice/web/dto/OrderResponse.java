package com.example.orderservice.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID orderId,
    String customerName,
    String status,
    BigDecimal totalAmount,
    String cancelReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<OrderItemResponse> items
) {}
