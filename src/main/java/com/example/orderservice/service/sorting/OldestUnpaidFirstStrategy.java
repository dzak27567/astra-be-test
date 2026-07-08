package com.example.orderservice.service.sorting;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class OldestUnpaidFirstStrategy implements OrderSortStrategy {
    @Override
    public String name() { return "oldest-unpaid"; }

    @Override
    public Sort sort() {
        // ponytail: true oldest-unpaid would need a query filter for status=CREATED,
        // but as a sort strategy we sort by status ASC (CREATED first) then createdAt ASC
        return Sort.by(
            Sort.Order.asc("status"),
            Sort.Order.asc("createdAt")
        );
    }
}
