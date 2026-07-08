package com.example.orderservice.service.sorting;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class NewestFirstStrategy implements OrderSortStrategy {
    @Override
    public String name() { return "newest"; }

    @Override
    public Sort sort() { return Sort.by(Sort.Direction.DESC, "createdAt"); }
}
