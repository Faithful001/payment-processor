# Payment Processor

A payment authorization, capture, and refund simulator built with Spring Boot, RabbitMQ, and an event-driven architecture. It models the real-world messiness of payment processing: authorization, capture, retries, and asynchronous settlement, rather than treating a payment as a single atomic database write.

This service is designed to work alongside the [Ledger Engine](https://github.com/Faithful001/ledger-engine): once a payment is captured, this service publishes an event that the Ledger Engine consumes to record the actual double-entry transaction. See [Interconnected Services](#interconnected-services) below for how the two fit together.

## Core Concept

- A `Payment` moves through a defined lifecycle: `CREATED` → `AUTHORIZED` → `CAPTURED` → (`REFUNDED` or `DISPUTED`), or `FAILED` at any authorization/capture step.
- Every state transition is recorded as a `PaymentAttempt`, giving a full audit trail of retries and failures, not just the current status.
- Capturing a payment does not synchronously touch the ledger. Instead, it writes an event to a **transactional outbox**, guaranteeing the event is never lost even if the app crashes immediately after the state change.
- A background publisher polls the outbox and sends events to RabbitMQ. A listener on the other end consumes them and calls the Ledger Engine to record the settled transaction.

## Tech Stack

- **Java 17**
- **Spring Boot 4.1.0**
- **Spring Data JPA**: persistence layer
- **PostgreSQL**: primary datastore
- **RabbitMQ**: async event publishing, with dead-letter queue support for failed processing
- **Spring Security**: authentication/authorization
- **Lombok**: boilerplate reduction
- **springdoc-openapi**: Swagger UI / OpenAPI documentation
- **Maven**: build tool

## Architecture

```
com.king.paymentprocessor
├── domain
│   ├── payment       # Payment entity, state machine, attempts, API
│   ├── refund         # Refund entity and logic
│   └── outbox         # Transactional outbox pattern implementation
├── infrastructure
│   ├── messaging
│   │   └── rabbitmq   # Exchange, queue, and dead-letter queue configuration
│   └── client
│       └── LedgerClient  # HTTP client for calling the Ledger Engine
├── config             # Spring framework configuration (security, OpenAPI)
└── PaymentProcessorApplication
```

### Domain Model

| Entity | Description |
|---|---|
| `Payment` | The core record: amount, currency, status, source/destination accounts, idempotency key. |
| `PaymentAttempt` | One row per processing attempt against a payment, capturing the resulting status and any failure reason. |
| `Refund` | A full or partial refund against a captured payment. Multiple partial refunds can accumulate up to the captured amount. |
| `OutboxEvent` | A pending or published domain event, written in the same database transaction as the state change it represents. |

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/payments` | Create and authorize a new payment (requires `Idempotency-Key` header) |
| `POST` | `/payments/{id}/capture` | Capture a previously authorized payment |
| `GET` | `/payments/{id}` | Get a payment's current status |
| `GET` | `/payments/{id}/attempts` | Get the full processing attempt history for a payment |
| `POST` | `/payments/{paymentId}/refunds` | Create a full or partial refund against a captured payment |
| `GET` | `/payments/{paymentId}/refunds` | List all refunds for a payment |

## Async Event Flow

```
Payment captured
  → OutboxEvent saved (same DB transaction as the status change)
  → OutboxPublisher polls every 2s, publishes to RabbitMQ exchange
  → Exchange routes to payment-events-queue
  → PaymentCapturedListener consumes the message
  → LedgerClient calls the Ledger Engine's POST /transactions
  → Ledger Engine validates debits == credits and posts the transaction
```

If the Ledger Engine call fails, the listener rethrows the exception so RabbitMQ retries delivery. After repeated failures, the message is routed to `payment-events-queue.dlq` instead of being retried forever or silently dropped.

## Interconnected Services

This project does not stand alone. It is one half of a small distributed system:

```
┌─────────────────────┐         RabbitMQ          ┌─────────────────────┐
│  Payment Processor   │  ───── PaymentCaptured ──▶│   (event consumed   │
│  (this service)      │        event               │    by listener)    │
└─────────────────────┘                            └──────────┬──────────┘
                                                                │
                                                     HTTP POST  │  /transactions
                                                                ▼
                                                     ┌─────────────────────┐
                                                     │   Ledger Engine     │
                                                     │  (separate service) │
                                                     └─────────────────────┘
```

- **Payment Processor** owns the payment lifecycle (authorization, capture, refunds) and never writes ledger data directly.
- **Ledger Engine** owns the source-of-truth double-entry accounting record and knows nothing about payments, authorization, or retries. It only ever sees a `POST /transactions` request with balanced debit/credit entries.
- The two communicate exclusively through the `PaymentCaptured` event published to RabbitMQ, followed by a synchronous HTTP call from the payment side to the ledger side. Neither service shares a database with the other.
- Each service can be run, tested, and deployed independently. The Ledger Engine has no dependency on the Payment Processor existing at all; it is a general-purpose ledger that could accept transactions from any source.

**To run both services together locally:**
1. Start Postgres and RabbitMQ (see [Getting Started](#getting-started) below)
2. Run the Ledger Engine on its own port (e.g. `server.port=8081`)
3. Run this Payment Processor on a different port (e.g. `server.port=8080`)
4. Set `ledger.engine.base-url` in this project's `application.properties` to point at the running Ledger Engine
5. Capture a payment here and watch the transaction appear in the Ledger Engine's `/accounts/{id}/entries`

## Getting Started

### Prerequisites

- Java 17+
- Maven (or use the included `./mvnw` wrapper)
- PostgreSQL running locally
- RabbitMQ running locally

### Configuration

Set your connection details in `src/main/resources/application.properties` (or via `.env`, loaded manually at startup):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/payment_processor
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update

spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=your_rabbitmq_user
spring.rabbitmq.password=your_rabbitmq_password

ledger.engine.base-url=http://localhost:8081
```

### Run the app

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`, and Swagger UI at `http://localhost:8080/swagger-ui/index.html`.

RabbitMQ's management UI is available at `http://localhost:15672` if running via the included `docker-compose.yml`, useful for watching messages flow through the queue in real time.

### Example: creating and capturing a payment

```bash
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{
    "sourceAccountId": "<source-account-id>",
    "destinationAccountId": "<destination-account-id>",
    "amount": 5000.00,
    "currency": "NGN"
  }'
```

```bash
curl -X POST http://localhost:8080/payments/<payment-id>/capture
```

Capturing the payment triggers the full async flow described above, within a few seconds, the corresponding transaction should appear in the Ledger Engine.

## Roadmap

- [x] Core payment lifecycle (create, authorize, capture)
- [x] Refunds (full and partial)
- [x] Transactional outbox pattern for reliable event publishing
- [x] RabbitMQ integration with dead-letter queue
- [x] Ledger Engine integration via async event + HTTP call
- [ ] Optimistic locking retry logic on concurrent capture attempts
- [ ] Compensating transactions for failed captures (release authorization hold)
- [ ] Webhook notifications on payment status changes
- [ ] Circuit breaker around the Ledger Engine HTTP call
- [ ] Rate limiting on payment creation per account

## Notes on Design Decisions

- **The outbox pattern, not direct publishing**, is used specifically to avoid the classic dual-write problem: if the app saved the payment status and then published directly to RabbitMQ, a crash between those two steps would lose the event permanently. Writing to the outbox in the same transaction as the state change makes event loss structurally impossible.
- **RabbitMQ over Kafka** was chosen deliberately for this project's scope. RabbitMQ's dead-letter queue is broker-level configuration, while Kafka requires the consumer to implement DLQ behavior in application code. For learning the Saga/event-driven pattern, the simpler broker-level behavior keeps the focus on the architecture rather than the messaging library's internals.
- **The Payment Processor never writes to the Ledger Engine's database directly.** All communication happens over HTTP, matching how independently deployable services in a real distributed system would interact.