# Tezza Lending Application — Design Spec
**Date:** 2026-09-07  
**Status:** Approved

---

## 1. Overview

Java/Spring Boot lending application covering loan product configuration, loan management, customer profiles, and notifications. Evaluated on architecture, code quality, RESTful practices, domain modeling, and test coverage.

**Architecture decision:** Spring Modulith (modular monolith) — single deployable JAR with compile-time enforced module boundaries. Mirrors CO-OPBANK eloans-platform-2.0 patterns. Each module is a potential future microservice.

---

## 2. Tech Stack

| Concern | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.x |
| Modularity | Spring Modulith 1.3.x |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Auth | Spring Authorization Server (OAuth2 embedded) |
| Messaging | Apache Kafka |
| Email | JavaMailSender (SMTP) |
| SMS/Push | Stub dispatchers (log to console) |
| Docs | SpringDoc OpenAPI 3 |
| Boilerplate | Lombok + MapStruct |
| Testing | JUnit 5 + Mockito + Testcontainers + EmbeddedKafka |
| Coverage | JaCoCo 80% gate on service layer |
| Infrastructure | Docker Compose (PostgreSQL + Kafka + Zookeeper) |

---

## 3. Module Structure

```
com.tezza.lending/
├── shared/           # BaseAuditEntity, ApiResponse<T>, exceptions, enums — Type.OPEN
├── auth/             # Spring Authorization Server config, user registration
├── product/          # Loan product config, fees, tenure rules
├── customer/         # Customer profiles, loan limits
├── loan/             # Loan lifecycle, installments, billing cycles, sweep jobs
├── repayment/        # Repayment processing, balance calculation
└── notification/     # Kafka consumer, template engine, channel dispatchers
```

Each module internal layout:
```
<module>/
├── api/          # Public interfaces + event records + DTOs exposed to other modules
├── internal/     # Entities, repositories, service impls — invisible externally
└── web/          # @RestController classes
```

Cross-module access rule: only `api` subpackage is accessible. `internal` is hidden even if `public`. Enforced by `ApplicationModules.verify()` test.

---

## 4. Database Conventions

- All table names: **UPPERCASE** (e.g., `LOAN_PRODUCTS`, `CUSTOMERS`)
- All column names: **UPPERCASE** (e.g., `PRINCIPAL_AMOUNT`, `CREATED_AT`)
- JPA `@Table(name = "UPPERCASE")` + `@Column(name = "UPPERCASE")` on all entities
- Managed by Flyway migrations in `resources/db/migration/`

---

## 5. Domain Model

### 5.1 Shared

```
BaseAuditEntity (MappedSuperclass)
├── CREATED_AT    (LocalDateTime)
├── UPDATED_AT    (LocalDateTime)
├── CREATED_BY    (String)
└── UPDATED_BY    (String)
@EntityListeners(AuditingEntityListener)
```

**ApiResponse<T>:**
```java
int status;           // 0=success, 1=failed
String message;
String correlationId;
T data;
```

### 5.2 Product Module

**Table: LOAN_PRODUCTS**
```
ID                  UUID PK
NAME                VARCHAR(100) NOT NULL
DESCRIPTION         TEXT
TENURE_VALUE        INT NOT NULL
TENURE_TYPE         VARCHAR(10) NOT NULL  -- DAYS | MONTHS
MIN_AMOUNT          NUMERIC(19,2) NOT NULL
MAX_AMOUNT          NUMERIC(19,2) NOT NULL
GRACE_PERIOD_DAYS   INT DEFAULT 0
ACTIVE              BOOLEAN DEFAULT TRUE
CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
```

**Table: PRODUCT_FEES**
```
ID                  UUID PK
PRODUCT_ID          UUID FK → LOAN_PRODUCTS
FEE_TYPE            VARCHAR(20) NOT NULL  -- SERVICE_FEE | DAILY_FEE | LATE_FEE
CALCULATION_TYPE    VARCHAR(10) NOT NULL  -- FIXED | PERCENTAGE
AMOUNT              NUMERIC(19,2) NOT NULL
DAYS_AFTER_DUE      INT DEFAULT 0
ACTIVE              BOOLEAN DEFAULT TRUE
CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
```

### 5.3 Customer Module

**Table: CUSTOMERS**
```
ID                  UUID PK
FIRST_NAME          VARCHAR(100) NOT NULL
LAST_NAME           VARCHAR(100) NOT NULL
EMAIL               VARCHAR(150) UNIQUE NOT NULL
PHONE               VARCHAR(20)
NATIONAL_ID         VARCHAR(50) UNIQUE
STATUS              VARCHAR(20) DEFAULT 'ACTIVE'  -- ACTIVE | SUSPENDED | BLACKLISTED
CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
```

**Table: CUSTOMER_LOAN_LIMITS**
```
ID                  UUID PK
CUSTOMER_ID         UUID FK → CUSTOMERS UNIQUE
MIN_LIMIT           NUMERIC(19,2) NOT NULL
MAX_LIMIT           NUMERIC(19,2) NOT NULL
CURRENT_LIMIT       NUMERIC(19,2) NOT NULL
CREDIT_SCORE        INT DEFAULT 0
LAST_REVIEWED_AT    TIMESTAMP
CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
```

### 5.4 Loan Module

**Loan states:**
```
OPEN → OVERDUE → WRITTEN_OFF
OPEN → CLOSED
OPEN → CANCELLED
OVERDUE → CLOSED  (late repayment)
```

**Table: LOANS**
```
ID                      UUID PK
LOAN_NUMBER             VARCHAR(50) UNIQUE NOT NULL
CUSTOMER_ID             UUID FK → CUSTOMERS
PRODUCT_ID              UUID FK → LOAN_PRODUCTS
PRINCIPAL_AMOUNT        NUMERIC(19,2) NOT NULL
DISBURSED_AMOUNT        NUMERIC(19,2)
OUTSTANDING_BALANCE     NUMERIC(19,2) NOT NULL
STATUS                  VARCHAR(20) NOT NULL  -- OPEN|CLOSED|CANCELLED|OVERDUE|WRITTEN_OFF
LOAN_TYPE               VARCHAR(15) NOT NULL  -- LUMP_SUM | INSTALLMENT
BILLING_CYCLE_TYPE      VARCHAR(15) NOT NULL  -- INDIVIDUAL | CONSOLIDATED
CONSOLIDATED_DUE_DATE   DATE  -- nullable, used when BILLING_CYCLE_TYPE=CONSOLIDATED
DUE_DATE                DATE NOT NULL
DISBURSED_AT            TIMESTAMP
CLOSED_AT               TIMESTAMP
CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
```

**Table: LOAN_INSTALLMENTS**
```
ID                  UUID PK
LOAN_ID             UUID FK → LOANS
INSTALLMENT_NUMBER  INT NOT NULL
PRINCIPAL_AMOUNT    NUMERIC(19,2) NOT NULL
FEE_AMOUNT          NUMERIC(19,2) DEFAULT 0
TOTAL_AMOUNT        NUMERIC(19,2) NOT NULL
OUTSTANDING_AMOUNT  NUMERIC(19,2) NOT NULL
DUE_DATE            DATE NOT NULL
PAID_AT             TIMESTAMP
STATUS              VARCHAR(10) NOT NULL  -- PENDING | PAID | OVERDUE
CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
```

### 5.5 Repayment Module

**Table: REPAYMENTS**
```
ID              UUID PK
LOAN_ID         UUID FK → LOANS
AMOUNT          NUMERIC(19,2) NOT NULL
REFERENCE       VARCHAR(100) UNIQUE NOT NULL
CHANNEL         VARCHAR(20) NOT NULL  -- MPESA | BANK | CASH
PROCESSED_AT    TIMESTAMP NOT NULL
CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
```

### 5.6 Notification Module

**Table: NOTIFICATION_TEMPLATES**
```
ID              UUID PK
EVENT_TYPE      VARCHAR(50) NOT NULL  -- LOAN_CREATED|DUE_REMINDER|REPAYMENT_ACK|OVERDUE_NOTICE
CHANNEL         VARCHAR(10) NOT NULL  -- EMAIL | SMS | PUSH
SUBJECT         VARCHAR(200)
BODY_TEMPLATE   TEXT NOT NULL         -- supports {{variable}} placeholders
ACTIVE          BOOLEAN DEFAULT TRUE
CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
```

**Table: NOTIFICATION_RULES**
```
ID                  UUID PK
PRODUCT_ID          UUID FK → LOAN_PRODUCTS  -- nullable = applies to all products
CUSTOMER_SEGMENT    VARCHAR(50)              -- nullable = applies to all segments
EVENT_TYPE          VARCHAR(50) NOT NULL
CHANNEL             VARCHAR(10) NOT NULL
ENABLED             BOOLEAN DEFAULT TRUE
PRIORITY            INT DEFAULT 0
DELAY_MINUTES       INT DEFAULT 0
CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
```

**Table: NOTIFICATION_LOGS**
```
ID              UUID PK
CUSTOMER_ID     UUID FK → CUSTOMERS
EVENT_TYPE      VARCHAR(50) NOT NULL
CHANNEL         VARCHAR(10) NOT NULL
RECIPIENT       VARCHAR(200) NOT NULL
STATUS          VARCHAR(10) NOT NULL  -- SENT | FAILED | PENDING
ERROR_MESSAGE   TEXT
SENT_AT         TIMESTAMP
CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
```

---

## 6. API Design

All endpoints return `ApiResponse<T>`. Errors via `@ControllerAdvice`.  
Auth: Bearer token (OAuth2, Spring Authorization Server).

### Auth
```
POST /auth/register          → Register customer account
POST /oauth2/token           → Obtain access token (password / client_credentials grant)
```

### Products
```
POST   /api/v1/products                      → Create loan product         [ADMIN]
GET    /api/v1/products                      → List products (paginated)   [ANY]
GET    /api/v1/products/{id}                 → Get product                 [ANY]
PUT    /api/v1/products/{id}                 → Update product              [ADMIN]
DELETE /api/v1/products/{id}                 → Deactivate product          [ADMIN]
POST   /api/v1/products/{id}/fees            → Add fee to product          [ADMIN]
DELETE /api/v1/products/{id}/fees/{feeId}    → Remove fee                  [ADMIN]
```

### Customers
```
POST   /api/v1/customers                        → Create customer           [ADMIN]
GET    /api/v1/customers/{id}                   → Get customer profile      [ADMIN|SELF]
PUT    /api/v1/customers/{id}                   → Update customer           [ADMIN|SELF]
GET    /api/v1/customers/{id}/loan-limit        → Get loan limit            [ADMIN]
PUT    /api/v1/customers/{id}/loan-limit        → Update loan limit         [ADMIN]
GET    /api/v1/customers/{id}/loans             → Customer loan history     [ADMIN|SELF]
```

### Loans
```
POST   /api/v1/loans                            → Apply & disburse loan     [CUSTOMER]
GET    /api/v1/loans/{id}                       → Get loan details          [ADMIN|OWNER]
GET    /api/v1/loans/{id}/installments          → Installment schedule      [ADMIN|OWNER]
POST   /api/v1/loans/{id}/cancel                → Cancel loan               [ADMIN]
POST   /api/v1/loans/{id}/write-off             → Write off loan            [ADMIN]
GET    /api/v1/loans?status=OVERDUE&page=0      → Filter loans by status    [ADMIN]
```

### Repayments
```
POST   /api/v1/repayments                       → Process repayment         [CUSTOMER|ADMIN]
GET    /api/v1/loans/{id}/repayments            → Loan repayment history    [ADMIN|OWNER]
```

### Notifications
```
POST   /api/v1/notifications/templates          → Create template           [ADMIN]
GET    /api/v1/notifications/templates          → List templates            [ADMIN]
PUT    /api/v1/notifications/templates/{id}     → Update template           [ADMIN]
POST   /api/v1/notifications/rules              → Create notification rule  [ADMIN]
GET    /api/v1/notifications/logs/{customerId}  → Customer notification log [ADMIN]
```

---

## 7. Event & Notification Flow

```
Loan action (disburse / overdue / repayment)
  → LoanEvent (Spring ApplicationEvent)
    → KafkaProducer → topic: lending.notifications
      → KafkaConsumer (notification module)
        → NotificationRuleEvaluator (product + segment filter)
          → TemplateRenderer ({{variable}} substitution)
            → EmailDispatcher   (JavaMailSender — real SMTP)
            → SmsDispatcher     (stub — console log)
            → PushDispatcher    (stub — console log)
              → NotificationLog persisted
```

**Kafka topics:**
- `lending.notifications` — notification event payloads
- `lending.loan-events` — audit trail (future downstream use)

---

## 8. Sweep Jobs

All in loan module, `@Scheduled`:

| Job | Cron | Action |
|---|---|---|
| OverdueSweepJob | `0 5 0 * * *` (00:05 daily) | Find OPEN loans past dueDate → set OVERDUE → apply late fees → publish LoanOverdueEvent |
| DailyFeeSweepJob | `0 10 0 * * *` (00:10 daily) | Find OPEN/OVERDUE loans with DAILY_FEE → accrue to OUTSTANDING_BALANCE |
| DueDateReminderJob | `0 0 8 * * *` (08:00 daily) | Find loans due in 3 days → publish DueDateReminderEvent |

---

## 9. Testing Strategy

| Layer | Scope | Tool |
|---|---|---|
| Unit | Service logic: fee calc, installment generation, repayment allocation, template rendering | JUnit 5 + Mockito |
| Integration | Repository queries, Flyway migrations, full request cycle | `@SpringBootTest` + Testcontainers (PostgreSQL) |
| Module boundary | Spring Modulith rules enforced at build | `ApplicationModules.verify()` |
| API | Controller endpoints, OAuth2 auth flow | `@WebMvcTest` + MockMvc |
| Kafka | Event publish/consume | `@EmbeddedKafka` |
| Seed data | Full demo of all use cases | `V99__seed_data.sql` Flyway migration |

JaCoCo: 80% line coverage gate on service layer.

---

## 10. Project Structure

```
tezza-lending/
├── docker-compose.yml
├── README.md
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/tezza/lending/
    │   │   ├── LendingApplication.java
    │   │   ├── shared/
    │   │   ├── auth/
    │   │   ├── product/
    │   │   │   ├── api/
    │   │   │   ├── internal/
    │   │   │   └── web/
    │   │   ├── customer/
    │   │   ├── loan/
    │   │   ├── repayment/
    │   │   └── notification/
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    │           ├── V1__create_products.sql
    │           ├── V2__create_customers.sql
    │           ├── V3__create_loans.sql
    │           ├── V4__create_repayments.sql
    │           ├── V5__create_notifications.sql
    │           └── V99__seed_data.sql
    └── test/
        └── java/com/tezza/lending/
            ├── ModularityTest.java
            ├── product/
            ├── customer/
            ├── loan/
            ├── repayment/
            └── notification/
```
