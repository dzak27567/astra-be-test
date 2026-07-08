package com.example.orderservice.web.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
    String productName,
    Integer quantity,
    BigDecimal unitPrice
) {}
