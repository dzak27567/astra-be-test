package com.example.orderservice.service;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderItem;
import com.example.orderservice.domain.OrderStatus;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest validCreateRequest;
    private UUID existingOrderId;
    private Order existingOrder;

    @BeforeEach
    void setUp() {
        validCreateRequest = new CreateOrderRequest(
                "Andi Wijaya",
                List.of(
                        new OrderItemRequest("Apple", 3, new BigDecimal("0.50")),
                        new OrderItemRequest("Bread Loaf", 1, new BigDecimal("2.20"))
                )
        );

        existingOrderId = UUID.randomUUID();
        existingOrder = new Order();
        existingOrder.setId(existingOrderId);
        existingOrder.setCustomerName("Andi Wijaya");
        existingOrder.setStatus(OrderStatus.CREATED);
        existingOrder.setTotalAmount(new BigDecimal("3.70"));

        OrderItem apple = new OrderItem();
        apple.setProductName("Apple");
        apple.setQuantity(3);
        apple.setUnitPrice(new BigDecimal("0.50"));
        existingOrder.addItem(apple);

        OrderItem bread = new OrderItem();
        bread.setProductName("Bread Loaf");
        bread.setQuantity(1);
        bread.setUnitPrice(new BigDecimal("2.20"));
        existingOrder.addItem(bread);
    }

    // --- Create ---

    @Test
    void createOrder_success_computesTotalServerSide() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        OrderResponse response = orderService.createOrder(validCreateRequest);

        assertNotNull(response.orderId());
        assertEquals("Andi Wijaya", response.customerName());
        assertEquals("CREATED", response.status());
        assertEquals(new BigDecimal("3.70"), response.totalAmount());
        assertEquals(2, response.items().size());
    }

    @Test
    void createOrder_ignoresClientTotal_computesServerSide() {
        // Even if client somehow sends a total, server computes its own
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        OrderResponse response = orderService.createOrder(validCreateRequest);

        // 3 * 0.50 + 1 * 2.20 = 3.70
        assertEquals(new BigDecimal("3.70"), response.totalAmount());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertEquals(new BigDecimal("3.70"), captor.getValue().getTotalAmount());
    }

    // --- Read ---

    @Test
    void getOrder_success() {
        when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(existingOrder));

        OrderResponse response = orderService.getOrder(existingOrderId);

        assertEquals(existingOrderId, response.orderId());
        assertEquals("Andi Wijaya", response.customerName());
    }

    @Test
    void getOrder_notFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(unknownId));
    }

    // --- List ---

    @Test
    void listOrders_defaultSort_newest() {
        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existingOrder)));

        Page<OrderResponse> page = orderService.listOrders(0, 20, "newest");

        assertEquals(1, page.getTotalElements());
        verify(orderRepository).findAll(any(Pageable.class));
    }

    @Test
    void listOrders_nullSort_defaultsToNewest() {
        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        orderService.listOrders(0, 20, null);

        verify(orderRepository).findAll(any(Pageable.class));
    }

    // --- Update ---

    @Test
    void updateOrder_success_recomputesTotal() {
        when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(
                "Updated Name",
                List.of(new OrderItemRequest("Milk", 2, new BigDecimal("3.00")))
        );

        OrderResponse response = orderService.updateOrder(existingOrderId, updateRequest);

        assertEquals("Updated Name", response.customerName());
        assertEquals(new BigDecimal("6.00"), response.totalAmount());
        assertEquals(1, response.items().size());
    }

    @Test
    void updateOrder_notFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

        UpdateOrderRequest request = new UpdateOrderRequest(
                "Name",
                List.of(new OrderItemRequest("X", 1, BigDecimal.ONE))
        );

        assertThrows(OrderNotFoundException.class, () -> orderService.updateOrder(unknownId, request));
    }

    // --- Delete ---

    @Test
    void deleteOrder_success() {
        when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(existingOrder));

        orderService.deleteOrder(existingOrderId);

        verify(orderRepository).delete(existingOrder);
    }

    @Test
    void deleteOrder_notFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.deleteOrder(unknownId));
    }
}
