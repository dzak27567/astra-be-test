package com.example.orderservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record OrderItemRequest(
    @NotBlank(message = "productName is required")
    String productName,

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than 0")
    Integer quantity,

    @NotNull(message = "unitPrice is required")
    @Positive(message = "unitPrice must be greater than 0")
    BigDecimal unitPrice
) {}
