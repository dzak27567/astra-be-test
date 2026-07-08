package com.example.orderservice.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelRequest(
    @NotBlank(message = "reason is required")
    String reason
) {}
