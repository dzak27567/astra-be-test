# Order Service — Backend Take-Home Assessment

A RESTful Order Service for a simple e-commerce shop built with **Spring Boot 4.1.0** and **Java 21**.

## Versions

| Component   | Version |
|-------------|---------|
| Java        | 21 (LTS) |
| Spring Boot | 4.1.0   |
| Build tool  | Maven   |
| Database    | H2 (file mode — data persists across requests) |

## Build, Run, and Test

```bash
# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run
# or
java -jar target/order-service-0.0.1-SNAPSHOT.jar

# Test (single command)
./mvnw test
```

The service starts on `http://localhost:8080`.

### Manual Testing Options:

1. **Swagger UI (Interactive Browser Console)**
   - Start the service and open: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
   - You can test and execute all endpoints directly from your browser.

2. **Postman Collection**
   - Import the file `Order_Service_Postman_Collection.json` located at the root of the project into Postman.
   - Set the `order_id` collection variable after creating an order to test other routes easily.

3. **H2 Database Console**
   - Access: `/h2-console`
   - JDBC URL: `jdbc:h2:file:./data/orderdb` (username: `sa`, password: empty)

## API Reference

### Create an order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Andi Wijaya",
    "items": [
      { "productName": "Apple", "quantity": 3, "unitPrice": 0.50 },
      { "productName": "Bread Loaf", "quantity": 1, "unitPrice": 2.20 }
    ]
  }'
```

Response: `201 Created` with server-assigned `orderId`, `status: CREATED`, `totalAmount: 3.70`.

### Read an order

```bash
curl http://localhost:8080/api/orders/{id}
curl http://localhost:8080/api/orders/{unknown-id}  # → 404
```

### List orders (paginated + sorted)

```bash
curl "http://localhost:8080/api/orders?page=0&size=20&sort=newest"
```

Available sort values: `newest` (default), `oldest`, `highest-total`, `oldest-unpaid`.

Adding a new sort rule requires only creating a new `@Component` implementing `OrderSortStrategy` — zero changes to existing code.

### Update an order

```bash
curl -X PUT http://localhost:8080/api/orders/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Updated Name",
    "items": [{ "productName": "Milk", "quantity": 2, "unitPrice": 3.00 }]
  }'
```

**Note:** Items cannot be modified once the order is `PAID` or beyond (returns `409`).

### Delete an order

```bash
curl -X DELETE http://localhost:8080/api/orders/{id}  # → 204 No Content
```

### Status transitions

```bash
curl -X POST http://localhost:8080/api/orders/{id}/pay      # CREATED → PAID
curl -X POST http://localhost:8080/api/orders/{id}/ship     # PAID → SHIPPED
curl -X POST http://localhost:8080/api/orders/{id}/deliver  # SHIPPED → DELIVERED
curl -X POST http://localhost:8080/api/orders/{id}/cancel \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Customer changed their mind" }'         # → CANCELLED
```

Illegal transitions return `409 Conflict`. Cancel without a `reason` returns `400`.

### Error response format

All errors follow a consistent structure:

```json
{
  "timestamp": "2026-07-08T10:00:00",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "items: items must not be empty",
  "path": "/api/orders"
}
```

| Status | Error code       | When                                         |
|--------|------------------|----------------------------------------------|
| 400    | VALIDATION_ERROR | Invalid input (empty items, negative qty)    |
| 400    | MALFORMED_REQUEST| Missing or unparseable JSON body             |
| 400    | INVALID_PARAMETER| Invalid path parameter (e.g., non-UUID id)   |
| 404    | NOT_FOUND        | Order ID does not exist                      |
| 409    | INVALID_STATE    | Illegal transition or immutable items edit   |
| 500    | INTERNAL_ERROR   | Unexpected server error (no stack trace leak)|

## Design Decisions

### Assumptions

- **`totalAmount` is server-computed.** `sum(quantity × unitPrice)` for all line items, using `BigDecimal` with scale 2 and `HALF_UP` rounding. Client-supplied totals are ignored — this prevents inconsistencies and is a security measure against tampering.
- **Currency is not modelled.** A single implicit currency is assumed (e.g., USD). Multi-currency would require a `currency` field and exchange-rate handling — out of scope.
- **Data persists across requests** via H2 file mode (`jdbc:h2:file:./data/orderdb`). Data does not survive `ddl-auto=create` but survives with current `ddl-auto=update`. This is stated as an assumption.
- **Delete is hard delete** and is permitted on any status. A soft-delete approach would be more production-appropriate but adds complexity beyond the spec.
- **Cancel is allowed from CREATED, PAID, and SHIPPED** — not from DELIVERED or CANCELLED (terminal states).

### How Part 2 requirements affected Part 1 design

- **`OrderStatus` was designed as a state machine from the start.** The enum contains a transition table (`canTransitionTo`) so the logic is centralized and easily testable.
- **Separate endpoints for each transition** (`/pay`, `/ship`, `/deliver`, `/cancel`) instead of `PATCH { status }`. This was chosen because cancellation requires additional data (`reason`), and the spec says "assume such rules will continue to be added" — action-specific endpoints with their own payloads are more extensible than a generic status field.
- **Items immutability** is enforced at the service layer: `updateOrder` rejects changes for any status beyond `CREATED`.
- **Sorting uses Strategy Pattern** with auto-discovery: each sort rule is an `@Component` implementing `OrderSortStrategy`. Adding a new rule = creating one new class, no changes to existing code (Open/Closed Principle).

### Architecture — Design Patterns Used

| Pattern            | Where                         | Why                                                                 |
|--------------------|-------------------------------|---------------------------------------------------------------------|
| **Strategy**       | `OrderSortStrategy` + impls   | Pluggable sort rules, each is a Spring bean auto-registered         |
| **State machine**  | `OrderStatus.canTransitionTo` | Transition validation centralized in the enum, no scattered if/else |
| **DTO**            | `web/dto/*` records           | Prevents entity exposure, blocks client override of server fields   |

### Optional Assessments Implemented

#### Robustness

The service explicitly rejects the following inputs and states, all demonstrated with tests:

| Rejected condition                     | Response    | Test coverage                           |
|----------------------------------------|-------------|-----------------------------------------|
| Empty or missing item list             | 400         | `createOrder_emptyItems`                |
| Missing/blank customerName             | 400         | `createOrder_missingCustomerName`       |
| Negative or zero quantity              | 400         | `createOrder_negativeQuantity`          |
| Negative or zero unitPrice             | 400         | `createOrder_negativeUnitPrice`         |
| Missing productName in item            | 400         | `createOrder_missingProductName`        |
| Malformed/empty JSON body              | 400         | `createOrder_malformedJson`             |
| Invalid UUID path parameter            | 400         | `getOrder_invalidUuid`                  |
| Unknown order ID (GET/PUT/DELETE/pay)  | 404         | `getOrder_notFound`, etc.               |
| Ship before PAID                       | 409         | `shipFromCreated`                       |
| Cancel from DELIVERED/CANCELLED        | 409         | `cancelFromDelivered`, etc.             |
| Update items after PAID/SHIPPED/etc.   | 409         | `updateAfterPaid`, etc.                 |
| Cancel without reason                  | 400         | `cancelWithoutReason`                   |
| Client-supplied orderId/status/total   | Ignored     | `createOrder_serverIgnoresClientOrderId`|

Stack traces are never exposed to the client — a generic fallback handler returns `500 INTERNAL_ERROR` without implementation details.

#### Deployment

A multi-stage `Dockerfile` is provided:

```bash
# Build and run with Docker
docker build -t order-service .
docker run -p 8080:8080 order-service
```

- **Stage 1 (build):** `eclipse-temurin:21-jdk` with Maven wrapper — dependencies cached in a separate layer for fast rebuilds.
- **Stage 2 (run):** `eclipse-temurin:21-jre` — minimal runtime image.
- `.dockerignore` excludes build artifacts, data directory, and IDE files.

### Scope deliberately omitted

- **Authentication/Authorization** — not specified in the requirements.
- **Pagination metadata links** (HATEOAS) — Spring's `Page` response provides `totalElements`, `totalPages`, `number`, `size` which is sufficient.

### What I would improve given more time

- **Soft delete** with `deletedAt` timestamp instead of hard delete.
- **Optimistic locking** (`@Version`) to handle concurrent status transitions.
- **Separate "update customerName" from "update items"** — the spec hints items are immutable after payment but other fields remain editable. Currently both are in the same endpoint.
- **Query-based sorting for `oldest-unpaid`** — currently sorts by status enum ordinal + createdAt, but a proper implementation would filter to `status = CREATED` orders first.
- **Integration test with Testcontainers + Postgres** for production-like database testing.
- **Docker Compose** with a separate Postgres container for production-like persistence.
- **Health check endpoint** (`/actuator/health`) for container orchestration readiness probes.

## Test Suite

81 tests total, all passing:

- **OrderServiceTest** (24 tests): unit tests with Mockito — CRUD success/failure, all legal/illegal transitions, item immutability after payment.
- **OrderControllerIntegrationTest** (22 tests): full HTTP-level tests with MockMvc — request validation, error responses, status codes, full order lifecycle.
- **RobustnessTest** (34 tests): comprehensive rejection scenario tests organized in `@Nested` groups — input validation, missing resources, illegal transitions, item immutability, cancel validation, server-computed field protection.
- **OrderServiceApplicationTests** (1 test): Spring context loads.

```bash
./mvnw test
# Tests run: 81, Failures: 0, Errors: 0, Skipped: 0
```
