package com.example.orderservice.service.sorting;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class HighestTotalFirstStrategy implements OrderSortStrategy {
    @Override
    public String name() { return "highest-total"; }

    @Override
    public Sort sort() { return Sort.by(Sort.Direction.DESC, "totalAmount"); }
}
