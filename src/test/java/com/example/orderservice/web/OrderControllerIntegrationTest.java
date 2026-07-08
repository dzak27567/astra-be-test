package com.example.orderservice.web;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderItem;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OrderControllerIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private OrderRepository orderRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        orderRepository.deleteAll();
    }

    // --- Create ---

    @Test
    void createOrder_valid_returns201() throws Exception {
        String body = """
                {
                    "customerName": "Andi Wijaya",
                    "items": [
                        { "productName": "Apple", "quantity": 3, "unitPrice": 0.50 },
                        { "productName": "Bread Loaf", "quantity": 1, "unitPrice": 2.20 }
                    ]
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.customerName").value("Andi Wijaya"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(3.70))
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void createOrder_emptyItems_returns400() throws Exception {
        String body = """
                {
                    "customerName": "Andi",
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createOrder_missingCustomerName_returns400() throws Exception {
        String body = """
                {
                    "items": [{ "productName": "X", "quantity": 1, "unitPrice": 1.00 }]
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createOrder_negativeQuantity_returns400() throws Exception {
        String body = """
                {
                    "customerName": "Andi",
                    "items": [{ "productName": "Apple", "quantity": -1, "unitPrice": 1.00 }]
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createOrder_zeroQuantity_returns400() throws Exception {
        String body = """
                {
                    "customerName": "Andi",
                    "items": [{ "productName": "Apple", "quantity": 0, "unitPrice": 1.00 }]
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- Read ---

    @Test
    void getOrder_exists_returns200() throws Exception {
        Order order = createTestOrder();

        mockMvc.perform(get("/api/orders/" + order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getId().toString()))
                .andExpect(jsonPath("$.customerName").value("Test"));
    }

    @Test
    void getOrder_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // --- List ---

    @Test
    void listOrders_empty_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listOrders_withData_returnsPaginated() throws Exception {
        createTestOrder();
        createTestOrder();

        mockMvc.perform(get("/api/orders?page=0&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    // --- Update ---

    @Test
    void updateOrder_valid_returns200() throws Exception {
        Order order = createTestOrder();

        String body = """
                {
                    "customerName": "Updated Name",
                    "items": [{ "productName": "Milk", "quantity": 2, "unitPrice": 3.00 }]
                }
                """;

        mockMvc.perform(put("/api/orders/" + order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Updated Name"))
                .andExpect(jsonPath("$.totalAmount").value(6.00))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void updateOrder_notFound_returns404() throws Exception {
        String body = """
                {
                    "customerName": "X",
                    "items": [{ "productName": "Y", "quantity": 1, "unitPrice": 1.00 }]
                }
                """;

        mockMvc.perform(put("/api/orders/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- Delete ---

    @Test
    void deleteOrder_exists_returns204() throws Exception {
        Order order = createTestOrder();

        mockMvc.perform(delete("/api/orders/" + order.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/" + order.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOrder_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/orders/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // --- Helper ---

    private Order createTestOrder() {
        Order order = new Order();
        order.setCustomerName("Test");
        order.setTotalAmount(new BigDecimal("1.00"));

        OrderItem item = new OrderItem();
        item.setProductName("Item");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("1.00"));
        order.addItem(item);

        return orderRepository.save(order);
    }
}
