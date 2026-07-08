package com.example.orderservice.service.sorting;

import org.springframework.data.domain.Sort;

public interface OrderSortStrategy {
    String name();
    Sort sort();
}
