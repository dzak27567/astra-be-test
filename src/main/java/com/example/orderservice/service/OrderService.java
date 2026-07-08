package com.example.orderservice.service;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderItem;
import com.example.orderservice.domain.OrderStatus;
import com.example.orderservice.exception.InvalidOrderStateException;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.sorting.OrderSortStrategy;
import com.example.orderservice.web.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final Map<String, OrderSortStrategy> sortStrategies;

    public OrderService(OrderRepository orderRepository, List<OrderSortStrategy> strategies) {
        this.orderRepository = orderRepository;
        this.sortStrategies = strategies.stream()
                .collect(Collectors.toMap(OrderSortStrategy::name, Function.identity()));
    }

    // --- CRUD ---

    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerName(request.customerName());

        for (OrderItemRequest itemReq : request.items()) {
            OrderItem item = new OrderItem();
            item.setProductName(itemReq.productName());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(itemReq.unitPrice());
            order.addItem(item);
        }

        order.setTotalAmount(computeTotal(order.getItems()));
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = findOrderOrThrow(id);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            sort = "newest";
        }

        OrderSortStrategy strategy = sortStrategies.get(sort);
        Sort sortObj = (strategy != null) ? strategy.sort() : sortStrategies.get("newest").sort();

        Page<Order> orders = orderRepository.findAll(PageRequest.of(page, size, sortObj));
        return orders.map(this::toResponse);
    }

    public OrderResponse updateOrder(UUID id, UpdateOrderRequest request) {
        Order order = findOrderOrThrow(id);

        // Items are immutable after payment
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderStateException(
                    "Cannot modify items: order is " + order.getStatus());
        }

        order.setCustomerName(request.customerName());
        order.getItems().clear();

        for (OrderItemRequest itemReq : request.items()) {
            OrderItem item = new OrderItem();
            item.setProductName(itemReq.productName());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(itemReq.unitPrice());
            order.addItem(item);
        }

        order.setTotalAmount(computeTotal(order.getItems()));
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    public void deleteOrder(UUID id) {
        Order order = findOrderOrThrow(id);
        orderRepository.delete(order);
    }

    // --- Status transitions ---

    public OrderResponse payOrder(UUID id) {
        return transition(id, OrderStatus.PAID);
    }

    public OrderResponse shipOrder(UUID id) {
        return transition(id, OrderStatus.SHIPPED);
    }

    public OrderResponse deliverOrder(UUID id) {
        return transition(id, OrderStatus.DELIVERED);
    }

    public OrderResponse cancelOrder(UUID id, String reason) {
        Order order = findOrderOrThrow(id);

        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new InvalidOrderStateException(
                    "Cannot cancel order in status " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    // --- Helpers ---

    private OrderResponse transition(UUID id, OrderStatus target) {
        Order order = findOrderOrThrow(id);

        if (!order.getStatus().canTransitionTo(target)) {
            throw new InvalidOrderStateException(
                    "Cannot transition from " + order.getStatus() + " to " + target);
        }

        order.setStatus(target);
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    private Order findOrderOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCancelReason(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses
        );
    }

    private BigDecimal computeTotal(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
