package com.example.orderservice.web;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderItem;
import com.example.orderservice.domain.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Robustness tests: explicitly verify every input/state the service must reject.
 * Each test documents a specific rejection scenario and the expected error response.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RobustnessTest {

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

    // ========== Input Validation ==========

    @Nested
    @DisplayName("Input validation — rejected requests")
    class InputValidation {

        @Test
        @DisplayName("Empty item list → 400")
        void createOrder_emptyItems() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "customerName": "Andi", "items": [] }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message", containsString("items")));
        }

        @Test
        @DisplayName("Missing customerName → 400")
        void createOrder_missingCustomerName() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "items": [{ "productName": "X", "quantity": 1, "unitPrice": 1.00 }] }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message", containsString("customerName")));
        }

        @Test
        @DisplayName("Blank customerName → 400")
        void createOrder_blankCustomerName() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "customerName": "  ", "items": [{ "productName": "X", "quantity": 1, "unitPrice": 1.00 }] }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Negative quantity → 400")
        void createOrder_negativeQuantity() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "customerName": "Andi", "items": [{ "productName": "X", "quantity": -5, "unitPrice": 1.00 }] }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message", containsString("quantity")));
        }

        @Test
        @DisplayName("Zero quantity → 400")
        void createOrder_zeroQuantity() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "customerName": "Andi", "items": [{ "productName": "X", "quantity": 0, "unitPrice": 1.00 }] }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Negative unitPrice → 400")
        void createOrder_negativeUnitPrice() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "customerName": "Andi", "items": [{ "productName": "X", "quantity": 1, "unitPrice": -2.00 }] }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message", containsString("unitPrice")));
        }

        @Test
        @DisplayName("Zero unitPrice → 400")
        void createOrder_zeroUnitPrice() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "customerName": "Andi", "items": [{ "productName": "X", "quantity": 1, "unitPrice": 0 }] }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Missing productName in item → 400")
        void createOrder_missingProductName() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "customerName": "Andi", "items": [{ "quantity": 1, "unitPrice": 1.00 }] }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message", containsString("productName")));
        }

        @Test
        @DisplayName("Malformed JSON body → 400")
        void createOrder_malformedJson() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not valid json }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
        }

        @Test
        @DisplayName("Empty request body → 400")
        void createOrder_emptyBody() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
        }

        @Test
        @DisplayName("Invalid UUID in path → 400")
        void getOrder_invalidUuid() throws Exception {
            mockMvc.perform(get("/api/orders/not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
        }

        @Test
        @DisplayName("Update with empty items → 400")
        void updateOrder_emptyItems() throws Exception {
            Order order = createTestOrder();

            mockMvc.perform(put("/api/orders/" + order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "customerName": "Andi", "items": [] }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    // ========== Missing Resources ==========

    @Nested
    @DisplayName("Missing resources — 404 responses")
    class MissingResources {

        @Test
        @DisplayName("GET unknown order → 404")
        void getOrder_notFound() throws Exception {
            mockMvc.perform(get("/api/orders/00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message", containsString("Order not found")));
        }

        @Test
        @DisplayName("PUT unknown order → 404")
        void updateOrder_notFound() throws Exception {
            mockMvc.perform(put("/api/orders/00000000-0000-0000-0000-000000000000")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "customerName": "X", "items": [{ "productName": "Y", "quantity": 1, "unitPrice": 1.00 }] }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("DELETE unknown order → 404")
        void deleteOrder_notFound() throws Exception {
            mockMvc.perform(delete("/api/orders/00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("POST /pay unknown order → 404")
        void payOrder_notFound() throws Exception {
            mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000000000/pay"))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== Illegal State Transitions ==========

    @Nested
    @DisplayName("Illegal state transitions — 409 responses")
    class IllegalTransitions {

        @Test
        @DisplayName("Ship from CREATED → 409")
        void shipFromCreated() throws Exception {
            Order order = createTestOrder();

            mockMvc.perform(post("/api/orders/" + order.getId() + "/ship"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("INVALID_STATE"))
                    .andExpect(jsonPath("$.message", containsString("Cannot transition")));
        }

        @Test
        @DisplayName("Deliver from CREATED → 409")
        void deliverFromCreated() throws Exception {
            Order order = createTestOrder();

            mockMvc.perform(post("/api/orders/" + order.getId() + "/deliver"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("INVALID_STATE"));
        }

        @Test
        @DisplayName("Deliver from PAID → 409")
        void deliverFromPaid() throws Exception {
            Order order = createTestOrder();
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            mockMvc.perform(post("/api/orders/" + order.getId() + "/deliver"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("INVALID_STATE"));
        }

        @Test
        @DisplayName("Pay already PAID order → 409")
        void payFromPaid() throws Exception {
            Order order = createTestOrder();
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            mockMvc.perform(post("/api/orders/" + order.getId() + "/pay"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Cancel from DELIVERED → 409")
        void cancelFromDelivered() throws Exception {
            Order order = createTestOrder();
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);

            mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "reason": "Too late" }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("INVALID_STATE"));
        }

        @Test
        @DisplayName("Cancel from CANCELLED → 409")
        void cancelFromCancelled() throws Exception {
            Order order = createTestOrder();
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "reason": "Again" }
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Pay from DELIVERED → 409")
        void payFromDelivered() throws Exception {
            Order order = createTestOrder();
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);

            mockMvc.perform(post("/api/orders/" + order.getId() + "/pay"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Ship from DELIVERED → 409")
        void shipFromDelivered() throws Exception {
            Order order = createTestOrder();
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);

            mockMvc.perform(post("/api/orders/" + order.getId() + "/ship"))
                    .andExpect(status().isConflict());
        }
    }

    // ========== Item Immutability ==========

    @Nested
    @DisplayName("Item immutability after payment")
    class ItemImmutability {

        private final String updateBody = """
                { "customerName": "New", "items": [{ "productName": "New", "quantity": 1, "unitPrice": 1.00 }] }
                """;

        @Test
        @DisplayName("Update items after PAID → 409")
        void updateAfterPaid() throws Exception {
            Order order = createTestOrder();
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            mockMvc.perform(put("/api/orders/" + order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("INVALID_STATE"))
                    .andExpect(jsonPath("$.message", containsString("Cannot modify")));
        }

        @Test
        @DisplayName("Update items after SHIPPED → 409")
        void updateAfterShipped() throws Exception {
            Order order = createTestOrder();
            order.setStatus(OrderStatus.SHIPPED);
            orderRepository.save(order);

            mockMvc.perform(put("/api/orders/" + order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Update items after DELIVERED → 409")
        void updateAfterDelivered() throws Exception {
            Order order = createTestOrder();
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);

            mockMvc.perform(put("/api/orders/" + order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Update items after CANCELLED → 409")
        void updateAfterCancelled() throws Exception {
            Order order = createTestOrder();
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            mockMvc.perform(put("/api/orders/" + order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
                    .andExpect(status().isConflict());
        }
    }

    // ========== Cancel Validation ==========

    @Nested
    @DisplayName("Cancel requires reason")
    class CancelValidation {

        @Test
        @DisplayName("Cancel without reason → 400")
        void cancelWithoutReason() throws Exception {
            Order order = createTestOrder();

            mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "reason": "" }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message", containsString("reason")));
        }

        @Test
        @DisplayName("Cancel with blank reason → 400")
        void cancelWithBlankReason() throws Exception {
            Order order = createTestOrder();

            mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "reason": "   " }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Cancel without body → 400")
        void cancelWithoutBody() throws Exception {
            Order order = createTestOrder();

            mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
        }
    }

    // ========== Server-computed totalAmount ==========

    @Nested
    @DisplayName("Total amount is always server-computed")
    class TotalAmountComputation {

        @Test
        @DisplayName("Server ignores any client-supplied totalAmount field")
        void createOrder_serverComputesTotal() throws Exception {
            // Client sends totalAmount=999 but server should compute 3.70
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "customerName": "Andi",
                                        "totalAmount": 999.00,
                                        "items": [
                                            { "productName": "Apple", "quantity": 3, "unitPrice": 0.50 },
                                            { "productName": "Bread", "quantity": 1, "unitPrice": 2.20 }
                                        ]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalAmount").value(3.70));
        }

        @Test
        @DisplayName("Server ignores client-supplied orderId field")
        void createOrder_serverIgnoresClientOrderId() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "customerName": "Andi",
                                        "orderId": "00000000-0000-0000-0000-000000000001",
                                        "items": [{ "productName": "X", "quantity": 1, "unitPrice": 1.00 }]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderId").exists())
                    .andExpect(jsonPath("$.orderId").value(not("00000000-0000-0000-0000-000000000001")));
        }

        @Test
        @DisplayName("Server ignores client-supplied status field")
        void createOrder_serverIgnoresClientStatus() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "customerName": "Andi",
                                        "status": "DELIVERED",
                                        "items": [{ "productName": "X", "quantity": 1, "unitPrice": 1.00 }]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("CREATED"));
        }
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
