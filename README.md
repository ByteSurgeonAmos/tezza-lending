# Tezza Lending Application

A Spring Modulith lending platform covering loan product configuration, loan management, customer profiles, and event-driven notifications.

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
│  │ Allocate FIFO    │  │  Email (real) / SMS stub / Push stub│  │
│  └──────────────────┘  └────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
        │                           │
   PostgreSQL 16              Apache Kafka
   (Flyway migrations)        (lending.notifications)
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
| `PUT` | `/api/v1/customers/{id}/loan-limit` | ADMIN | Update loan limit |

### Loans
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/loans` | Any | Apply & disburse loan |
| `GET` | `/api/v1/loans` | ADMIN | List loans (filter by status) |
| `GET` | `/api/v1/loans/{id}` | Any | Get loan |
| `GET` | `/api/v1/loans/{id}/installments` | Any | Installment schedule |
| `POST` | `/api/v1/loans/{id}/cancel` | ADMIN | Cancel loan |
| `POST` | `/api/v1/loans/{id}/write-off` | ADMIN | Write off loan |
| `GET` | `/api/v1/loans/{id}/repayments` | Any | Repayment history |

### Repayments
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/repayments` | Any | Process repayment |

### Notifications
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/notifications/templates` | ADMIN | Create template |
| `GET` | `/api/v1/notifications/templates` | ADMIN | List templates |
| `PUT` | `/api/v1/notifications/templates/{id}` | ADMIN | Update template |
| `POST` | `/api/v1/notifications/rules` | ADMIN | Create rule |
| `GET` | `/api/v1/notifications/logs/{customerId}` | ADMIN | Customer notification log |

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
| OverdueSweepJob | Daily 00:05 | Marks past-due OPEN loans as OVERDUE, applies late fees |
| DailyFeeSweepJob | Daily 00:10 | Accrues daily fee on OPEN/OVERDUE loans |
| DueDateReminderJob | Daily 08:00 | Sends reminders for loans due in 3 days |

## Database Schema

All tables use **uppercase** naming. Key tables:

| Table | Module | Description |
|---|---|---|
| `LOAN_PRODUCTS` | product | Loan product definitions |
| `PRODUCT_FEES` | product | Fee configuration per product |
| `CUSTOMERS` | customer | Customer profiles |
| `CUSTOMER_LOAN_LIMITS` | customer | Credit limits per customer |
| `LOANS` | loan | Loan records |
| `LOAN_INSTALLMENTS` | loan | Installment schedules |
| `REPAYMENTS` | repayment | Repayment transactions |
| `NOTIFICATION_TEMPLATES` | notification | Message templates with `{{variable}}` placeholders |
| `NOTIFICATION_RULES` | notification | Dispatch rules per event/channel |
| `NOTIFICATION_LOGS` | notification | Sent notification audit trail |
| `OAUTH2_REGISTERED_CLIENT` | auth | OAuth2 client registrations |

Migrations: `src/main/resources/db/migration/V1–V6__*.sql`
Seed data: `src/main/resources/db/migration/V99__seed_data.sql`

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
- **3 customers**: Alice Wanjiku, Brian Omondi, Carol Njeri — all ACTIVE with configured limits
- **8 notification templates**: EMAIL + SMS for all 4 event types
- **8 notification rules**: All events active for EMAIL + SMS channels
- **1 admin OAuth2 client**: `tezza-admin-client` / `admin-secret`
- **1 mobile OAuth2 client**: `tezza-mobile-client` / `mobile-secret`
