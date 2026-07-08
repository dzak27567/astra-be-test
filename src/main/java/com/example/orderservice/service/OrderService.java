package com.example.orderservice.service;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderItem;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.web.dto.CreateOrderRequest;
import com.example.orderservice.web.dto.OrderItemRequest;
import com.example.orderservice.web.dto.OrderItemResponse;
import com.example.orderservice.web.dto.OrderResponse;
import com.example.orderservice.web.dto.UpdateOrderRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

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
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            sort = "newest";
        }

        Sort sortObj = switch (sort) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "highest-total" -> Sort.by(Sort.Direction.DESC, "totalAmount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Page<Order> orders = orderRepository.findAll(PageRequest.of(page, size, sortObj));
        return orders.map(this::toResponse);
    }

    public OrderResponse updateOrder(UUID id, UpdateOrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

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
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        orderRepository.delete(order);
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
