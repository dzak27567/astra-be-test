package com.example.orderservice.domain;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    // ponytail: simple map instead of a full state machine class hierarchy
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            CREATED, Set.of(PAID, CANCELLED),
            PAID, Set.of(SHIPPED, CANCELLED),
            SHIPPED, Set.of(DELIVERED, CANCELLED),
            DELIVERED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
