# Tezza Lending Application

A production-ready Spring Modulith lending platform covering loan product configuration, loan management, customer profiles, repayment processing, and event-driven notifications — backed by PostgreSQL stored procedures for business-critical operations.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    tezza-lending (Spring Modulith)               │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │  auth    │  │ product  │  │ customer │  │    loan      │   │
│  │          │  │          │  │          │  │              │   │
│  │ OAuth2   │  │ Products │  │ Profiles │  │ Lifecycle    │   │
│  │ Client   │  │ Fees     │  │ Limits   │  │ Installments │   │
│  │ Creds    │  │ Tenure   │  │          │  │ Sweep Jobs   │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────┬───────┘   │
│                                                     │Events     │
│  ┌──────────────────┐  ┌──────────────────────────▼─────────┐  │
│  │   repayment      │  │           notification              │  │
│  │                  │  │                                     │  │
│  │ Process payments │  │  Spring Events → Kafka →            │  │
│  │ FIFO via SP      │  │  Email (real) / SMS stub / Push stub│  │
│  └──────────────────┘  └────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
        │                           │
   PostgreSQL 16              Apache Kafka
   (Flyway migrations)        (lending.notifications)
   (Stored Procedures)
```

**Each module** exposes only its `api/` subpackage to other modules. `internal/` packages are hidden — enforced at build by Spring Modulith's `ApplicationModules.verify()`.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

## Quick Start

```bash
# 1. Clone and enter directory
git clone https://github.com/ByteSurgeonAmos/tezza-lending.git
cd tezza-lending

# 2. Start infrastructure (PostgreSQL + Kafka)
docker compose up -d

# 3. Configure email (optional — SMS/push work without it)
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=your@email.com
export MAIL_PASSWORD=your-app-password

# 4. Run the application
mvn spring-boot:run

# 5. Access Swagger UI
open http://localhost:8080/swagger-ui.html
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/tezza_lending` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `tezza` | Database username |
| `DB_PASSWORD` | `tezza_pass` | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `TEZZA_ADMIN_CLIENT_SECRET` | `admin-secret` | Admin OAuth2 client secret |
| `TEZZA_MOBILE_CLIENT_SECRET` | `mobile-secret` | Mobile OAuth2 client secret |
| `TEZZA_JWT_PRIVATE_KEY` | _(ephemeral)_ | Base64-encoded PKCS8 RSA private key. If unset, a new key is generated each restart (tokens invalidated on restart). Generate with: `openssl genrsa 2048 \| openssl pkcs8 -topk8 -nocrypt -outform DER \| base64` |
| `TEZZA_ISSUER_URI` | `http://localhost:8080` | OAuth2 issuer URI |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP server host |
| `MAIL_PORT` | `587` | SMTP server port |
| `MAIL_USERNAME` | _(empty)_ | SMTP username |
| `MAIL_PASSWORD` | _(empty)_ | SMTP password / app password |

If mail credentials are not set, email dispatch will log an error but SMS/push stubs continue working.

## OAuth2 Authentication

This application uses **OAuth2 client_credentials** (microservice pattern). No user-level login.

### Get Access Token

```bash
# Admin client (full access)
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "tezza-admin-client:admin-secret" \
  -d "grant_type=client_credentials&scope=ADMIN"

# Mobile/customer client (limited access)
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "tezza-mobile-client:mobile-secret" \
  -d "grant_type=client_credentials&scope=CUSTOMER"
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

### Use Token

```bash
curl http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

### Register a New Client

```bash
curl -X POST http://localhost:8080/auth/clients/register \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "my-service",
    "clientSecret": "my-strong-secret-16chars",
    "clientName": "My Integration Service",
    "scope": "CUSTOMER"
  }'
```

## API Reference

### Products
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/products` | ADMIN | Create loan product |
| `GET` | `/api/v1/products` | Any | List products (paginated) |
| `GET` | `/api/v1/products/{id}` | Any | Get product |
| `PUT` | `/api/v1/products/{id}` | ADMIN | Update product |
| `DELETE` | `/api/v1/products/{id}` | ADMIN | Deactivate product |
| `POST` | `/api/v1/products/{id}/fees` | ADMIN | Add fee to product |
| `DELETE` | `/api/v1/products/{id}/fees/{feeId}` | ADMIN | Remove fee |

### Customers
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/customers` | ADMIN | Create customer |
| `GET` | `/api/v1/customers/{id}` | ADMIN | Get customer |
| `PUT` | `/api/v1/customers/{id}` | ADMIN | Update customer |
| `GET` | `/api/v1/customers/{id}/loan-limit` | ADMIN | Get loan limit |
| `PUT` | `/api/v1/customers/{id}/loan-limit` | ADMIN | Set/update loan limit |

### Loans
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/loans` | Any | Apply & disburse loan |
| `GET` | `/api/v1/loans` | ADMIN | List loans (filter by status) |
| `GET` | `/api/v1/loans/{id}` | Any | Get loan |
| `GET` | `/api/v1/loans/{id}/installments` | Any | Installment schedule |
| `POST` | `/api/v1/loans/{id}/cancel` | ADMIN | Cancel loan |
| `POST` | `/api/v1/loans/{id}/write-off` | ADMIN | Write off loan |

### Repayments
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/repayments` | Any | Process repayment |
| `GET` | `/api/v1/loans/{loanId}/repayments` | Any | Repayment history |

### Notifications
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/notifications/templates` | ADMIN | Create template |
| `GET` | `/api/v1/notifications/templates` | ADMIN | List templates |
| `PUT` | `/api/v1/notifications/templates/{id}` | ADMIN | Update template |
| `POST` | `/api/v1/notifications/rules` | ADMIN | Create dispatch rule |
| `GET` | `/api/v1/notifications/logs/{customerId}` | ADMIN | Customer notification log |

Full interactive docs: `http://localhost:8080/swagger-ui.html`

## Postman Collection

A ready-to-run Postman collection is included at `tezza-lending.postman_collection.json`.

It covers the full loan lifecycle in sequence:

| Section | Requests | What it tests |
|---|---|---|
| Auth | 3 | Admin token, customer token, OAuth2 client registration |
| Products | 7 | Create, list, get, update, add fees, remove fee |
| Customers | 5 | Create, get, update, set/get loan limit |
| Loans | 7 | Disburse (installment + lump sum), list, get, installment schedule, cancel |
| Repayments | 6 | Disburse loan for repayment, 3 partial payments (MPESA/Bank/Cash), repayment history, product cleanup |
| Notifications | 5 | Create template, list, update, create rule, get logs |
| Health | 1 | Actuator health |

**34 requests · 22 assertions · 0 failures** (verified against live instance)

### Run with Newman (Postman CLI)

```bash
# Install Newman
npm install -g newman

# Run the collection
newman run tezza-lending.postman_collection.json --delay-request 300
```

### Import into Postman Desktop

File → Import → `tezza-lending.postman_collection.json`

All variables (tokens, IDs) are auto-populated by test scripts — just run the collection in order.

## Stored Procedures

Business-critical database operations are implemented as **PostgreSQL stored procedures** (migration `V7__create_procedures.sql`). This keeps atomicity and FIFO logic inside the database transaction, avoiding race conditions that would arise from application-level reads followed by writes.

| Procedure | Called From | Purpose |
|---|---|---|
| `proc_allocate_repayment(loan_id, amount)` | `RepaymentServiceImpl` | FIFO allocation of a payment across open installments, oldest-due-first |
| `proc_close_loan_if_paid(loan_id)` | `RepaymentServiceImpl` | Auto-closes loan when outstanding balance reaches zero |
| `proc_apply_late_fee(loan_id, fee_amount)` | `OverdueSweepJob` | Adds a late fee to the outstanding balance of an overdue loan |
| `proc_apply_daily_fee(loan_id, fee_amount)` | `DailyFeeSweepJob` | Accrues a daily fee on any open or overdue loan |
| `proc_run_overdue_sweep(OUT count)` | `OverdueSweepJob` | Bulk transitions all past-due OPEN loans to OVERDUE in one atomic statement; returns affected count |

### Why Stored Procedures?

- **Atomicity**: Fee accrual and status transitions are single SQL statements — no partial states if the JVM crashes mid-loop.
- **FIFO correctness**: `proc_allocate_repayment` locks and processes installments in due-date order inside one transaction. An application-level loop would require pessimistic locking across multiple round-trips.
- **Sweep performance**: `proc_run_overdue_sweep` updates thousands of loans in one `UPDATE … WHERE` statement instead of iterating them in Java.

### Invocation Pattern

```java
entityManager.createNativeQuery("CALL proc_allocate_repayment(:loanId, :amount)")
    .setParameter("loanId", loan.getId())
    .setParameter("amount", request.getAmount())
    .executeUpdate();
```

## Loan Lifecycle

```
OPEN ──────────── fully repaid ──────────▶ CLOSED
  │
  ├── past due date ──────────────────────▶ OVERDUE ──── write-off ──▶ WRITTEN_OFF
  │                                           │
  │                                           └── late payment ──────▶ CLOSED
  │
  └── cancelled before disbursement ────────▶ CANCELLED
```

## Sweep Jobs

| Job | Schedule | Action |
|---|---|---|
| `OverdueSweepJob` | Daily 00:05 | Marks past-due OPEN loans OVERDUE via `proc_run_overdue_sweep`; applies late fees via `proc_apply_late_fee` |
| `DailyFeeSweepJob` | Daily 00:10 | Accrues daily fee on OPEN/OVERDUE loans via `proc_apply_daily_fee` |
| `DueDateReminderJob` | Daily 08:00 | Publishes `DUE_REMINDER` events for loans due within 3 days |

## Database Schema

All tables use **uppercase** naming. Migrations run automatically on startup via Flyway.

| Table | Migration | Description |
|---|---|---|
| `LOAN_PRODUCTS` | V1 | Loan product definitions |
| `PRODUCT_FEES` | V1 | Fee configuration per product |
| `CUSTOMERS` | V2 | Customer profiles |
| `CUSTOMER_LOAN_LIMITS` | V2 | Credit limits per customer |
| `LOANS` | V3 | Loan records |
| `LOAN_INSTALLMENTS` | V3 | Installment schedules |
| `REPAYMENTS` | V4 | Repayment transactions |
| `NOTIFICATION_TEMPLATES` | V5 | Message templates with `{{variable}}` placeholders |
| `NOTIFICATION_RULES` | V5 | Dispatch rules per event/channel/product/segment |
| `NOTIFICATION_LOGS` | V5 | Sent notification audit trail |
| `OAUTH2_REGISTERED_CLIENT` | V6 | OAuth2 client registrations (JDBC-backed) |
| Stored procedures | V7 | `proc_allocate_repayment`, `proc_close_loan_if_paid`, `proc_apply_late_fee`, `proc_apply_daily_fee`, `proc_run_overdue_sweep` |
| `EVENT_PUBLICATION` | V100 | Spring Modulith async event tracking |

Seed data: `V99__seed_data.sql`

A single combined SQL file (`schema.sql`) merging all migrations in order is included for reference or manual database setup:

```bash
psql -U tezza -d tezza_lending -f schema.sql
```

## Running Tests

```bash
# All tests (Testcontainers auto-manages PostgreSQL)
mvn test

# Specific module
mvn test -Dtest="ProductServiceTest,LoanServiceTest"

# Coverage report
mvn verify
open target/site/jacoco/index.html

# Modularity check only
mvn test -Dtest="ModularityTest"
```

## Seed Data

Pre-loaded via `V99__seed_data.sql`:
- **2 loan products**: "Personal Quick Loan" (30-day, 5% service fee + KES 500 late fee) and "Business Term Loan" (12-month, 3% service fee + 0.1% daily fee)
- **3 customers**: Alice Wanjiku (`c1000000-…-001`), Brian Omondi (`c1000000-…-002`), Carol Njeri (`c1000000-…-003`) — all ACTIVE with configured limits
- **8 notification templates**: EMAIL + SMS for all 4 event types (LOAN_CREATED, DUE_REMINDER, REPAYMENT_ACK, OVERDUE_NOTICE)
- **8 notification rules**: All events active for EMAIL + SMS channels
- **1 admin OAuth2 client**: `tezza-admin-client` / `admin-secret`
- **1 mobile OAuth2 client**: `tezza-mobile-client` / `mobile-secret`
