# Tezza Lending — Part 1: Scaffold, Migrations, Shared Module

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bootstrap runnable Spring Modulith project with PostgreSQL, Flyway migrations, and shared infrastructure.

**Architecture:** Spring Modulith modular monolith. Single JAR. Module boundaries enforced at compile time.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Spring Modulith 1.3.1, PostgreSQL 16, Flyway, Kafka, Spring Authorization Server, Lombok, SpringDoc OpenAPI 2.6.0, Testcontainers 1.20.1, JaCoCo 0.8.12

---

## Task 1: Project Scaffold

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/resources/application.yml`
- Create: `src/main/java/com/tezza/lending/LendingApplication.java`
- Create: `src/test/java/com/tezza/lending/ModularityTest.java`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>

    <groupId>com.tezza</groupId>
    <artifactId>lending</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>tezza-lending</name>
    <description>Tezza Lending Application — loan products, management, customer profiles, notifications</description>

    <properties>
        <java.version>21</java.version>
        <spring-modulith.version>1.3.1</spring-modulith.version>
        <springdoc.version>2.6.0</springdoc.version>
        <testcontainers.version>1.20.1</testcontainers.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.modulith</groupId>
                <artifactId>spring-modulith-bom</artifactId>
                <version>${spring-modulith.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- OAuth2 Authorization Server (embedded AS) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
        </dependency>

        <!-- OAuth2 Resource Server (JWT validation) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Mail -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>

        <!-- Spring Modulith -->
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-starter-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-starter-jpa</artifactId>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Flyway -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- Kafka -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>

        <!-- OpenAPI / Swagger UI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Jackson JavaTime -->
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.12</version>
                <executions>
                    <execution>
                        <goals><goal>prepare-agent</goal></goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals><goal>report</goal></goals>
                    </execution>
                    <execution>
                        <id>check</id>
                        <phase>verify</phase>
                        <goals><goal>check</goal></goals>
                        <configuration>
                            <rules>
                                <rule>
                                    <element>PACKAGE</element>
                                    <includes>
                                        <include>com.tezza.lending.*.internal.service</include>
                                    </includes>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.80</minimum>
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create docker-compose.yml**

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    container_name: tezza-postgres
    environment:
      POSTGRES_DB: tezza_lending
      POSTGRES_USER: tezza
      POSTGRES_PASSWORD: tezza_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U tezza -d tezza_lending"]
      interval: 10s
      timeout: 5s
      retries: 5

  zookeeper:
    image: confluentinc/cp-zookeeper:7.7.0
    container_name: tezza-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.7.0
    container_name: tezza-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'

volumes:
  postgres_data:
```

- [ ] **Step 3: Create src/main/resources/application.yml**

```yaml
spring:
  application:
    name: tezza-lending

  datasource:
    url: jdbc:postgresql://localhost:5432/tezza_lending
    username: tezza
    password: tezza_pass
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: false

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: tezza-lending-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.tezza.lending.*"

  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

  security:
    oauth2:
      authorizationserver:
        issuer-uri: http://localhost:8080

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
  show-actuator: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

tezza:
  notification:
    due-reminder-days-before: 3

logging:
  level:
    com.tezza.lending: INFO
    org.springframework.security: WARN
    org.springframework.kafka: WARN
```

- [ ] **Step 4: Create application-test.yml** at `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:16:///tezza_lending_test
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  flyway:
    enabled: true
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers}
  jpa:
    hibernate:
      ddl-auto: validate
```

- [ ] **Step 5: Create LendingApplication.java**

```java
package com.tezza.lending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LendingApplication {
    public static void main(String[] args) {
        SpringApplication.run(LendingApplication.class, args);
    }
}
```

- [ ] **Step 6: Create ModularityTest.java**

```java
package com.tezza.lending;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {

    ApplicationModules modules = ApplicationModules.of(LendingApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void createModuleDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
```

- [ ] **Step 7: Verify structure compiles**

```bash
mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Start infrastructure**

```bash
docker compose up -d
docker compose ps
```

Expected: postgres, zookeeper, kafka all `healthy` / `running`

- [ ] **Step 9: Commit**

```bash
git add pom.xml docker-compose.yml src/main/resources/application.yml \
  src/test/resources/application-test.yml \
  src/main/java/com/tezza/lending/LendingApplication.java \
  src/test/java/com/tezza/lending/ModularityTest.java
git commit -m "feat: bootstrap Spring Modulith project scaffold"
```

---

## Task 2: Flyway Migrations

**Files:**
- Create: `src/main/resources/db/migration/V1__create_products.sql`
- Create: `src/main/resources/db/migration/V2__create_customers.sql`
- Create: `src/main/resources/db/migration/V3__create_loans.sql`
- Create: `src/main/resources/db/migration/V4__create_repayments.sql`
- Create: `src/main/resources/db/migration/V5__create_notifications.sql`
- Create: `src/main/resources/db/migration/V6__create_auth.sql`
- Create: `src/main/resources/db/migration/V99__seed_data.sql`

- [ ] **Step 1: Create V1__create_products.sql**

```sql
CREATE TABLE LOAN_PRODUCTS (
    ID                UUID          NOT NULL DEFAULT gen_random_uuid(),
    NAME              VARCHAR(100)  NOT NULL,
    DESCRIPTION       TEXT,
    TENURE_VALUE      INTEGER       NOT NULL,
    TENURE_TYPE       VARCHAR(10)   NOT NULL CHECK (TENURE_TYPE IN ('DAYS', 'MONTHS')),
    MIN_AMOUNT        NUMERIC(19,2) NOT NULL,
    MAX_AMOUNT        NUMERIC(19,2) NOT NULL,
    GRACE_PERIOD_DAYS INTEGER       NOT NULL DEFAULT 0,
    ACTIVE            BOOLEAN       NOT NULL DEFAULT TRUE,
    CREATED_AT        TIMESTAMP     NOT NULL DEFAULT NOW(),
    UPDATED_AT        TIMESTAMP     NOT NULL DEFAULT NOW(),
    CREATED_BY        VARCHAR(100),
    UPDATED_BY        VARCHAR(100),
    CONSTRAINT PK_LOAN_PRODUCTS PRIMARY KEY (ID),
    CONSTRAINT CHK_LP_AMOUNTS CHECK (MAX_AMOUNT >= MIN_AMOUNT),
    CONSTRAINT CHK_LP_TENURE CHECK (TENURE_VALUE > 0)
);

CREATE TABLE PRODUCT_FEES (
    ID               UUID          NOT NULL DEFAULT gen_random_uuid(),
    PRODUCT_ID       UUID          NOT NULL,
    FEE_TYPE         VARCHAR(20)   NOT NULL CHECK (FEE_TYPE IN ('SERVICE_FEE', 'DAILY_FEE', 'LATE_FEE')),
    CALCULATION_TYPE VARCHAR(10)   NOT NULL CHECK (CALCULATION_TYPE IN ('FIXED', 'PERCENTAGE')),
    AMOUNT           NUMERIC(19,2) NOT NULL CHECK (AMOUNT >= 0),
    DAYS_AFTER_DUE   INTEGER       NOT NULL DEFAULT 0,
    ACTIVE           BOOLEAN       NOT NULL DEFAULT TRUE,
    CREATED_AT       TIMESTAMP     NOT NULL DEFAULT NOW(),
    UPDATED_AT       TIMESTAMP     NOT NULL DEFAULT NOW(),
    CREATED_BY       VARCHAR(100),
    UPDATED_BY       VARCHAR(100),
    CONSTRAINT PK_PRODUCT_FEES PRIMARY KEY (ID),
    CONSTRAINT FK_PF_PRODUCT FOREIGN KEY (PRODUCT_ID) REFERENCES LOAN_PRODUCTS(ID)
);

CREATE INDEX IDX_PRODUCT_FEES_PRODUCT_ID ON PRODUCT_FEES(PRODUCT_ID);
```

- [ ] **Step 2: Create V2__create_customers.sql**

```sql
CREATE TABLE CUSTOMERS (
    ID           UUID         NOT NULL DEFAULT gen_random_uuid(),
    FIRST_NAME   VARCHAR(100) NOT NULL,
    LAST_NAME    VARCHAR(100) NOT NULL,
    EMAIL        VARCHAR(150) NOT NULL,
    PHONE        VARCHAR(20),
    NATIONAL_ID  VARCHAR(50),
    STATUS       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                              CHECK (STATUS IN ('ACTIVE', 'SUSPENDED', 'BLACKLISTED')),
    CREATED_AT   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UPDATED_AT   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CREATED_BY   VARCHAR(100),
    UPDATED_BY   VARCHAR(100),
    CONSTRAINT PK_CUSTOMERS PRIMARY KEY (ID),
    CONSTRAINT UQ_CUSTOMERS_EMAIL UNIQUE (EMAIL),
    CONSTRAINT UQ_CUSTOMERS_NATIONAL_ID UNIQUE (NATIONAL_ID)
);

CREATE TABLE CUSTOMER_LOAN_LIMITS (
    ID               UUID          NOT NULL DEFAULT gen_random_uuid(),
    CUSTOMER_ID      UUID          NOT NULL,
    MIN_LIMIT        NUMERIC(19,2) NOT NULL DEFAULT 0,
    MAX_LIMIT        NUMERIC(19,2) NOT NULL,
    CURRENT_LIMIT    NUMERIC(19,2) NOT NULL,
    CREDIT_SCORE     INTEGER       NOT NULL DEFAULT 0,
    LAST_REVIEWED_AT TIMESTAMP,
    CREATED_AT       TIMESTAMP     NOT NULL DEFAULT NOW(),
    UPDATED_AT       TIMESTAMP     NOT NULL DEFAULT NOW(),
    CREATED_BY       VARCHAR(100),
    UPDATED_BY       VARCHAR(100),
    CONSTRAINT PK_CUSTOMER_LOAN_LIMITS PRIMARY KEY (ID),
    CONSTRAINT FK_CLL_CUSTOMER FOREIGN KEY (CUSTOMER_ID) REFERENCES CUSTOMERS(ID),
    CONSTRAINT UQ_CLL_CUSTOMER UNIQUE (CUSTOMER_ID),
    CONSTRAINT CHK_CLL_LIMITS CHECK (MAX_LIMIT >= MIN_LIMIT AND CURRENT_LIMIT >= 0)
);

CREATE INDEX IDX_CUSTOMER_LOAN_LIMITS_CUSTOMER_ID ON CUSTOMER_LOAN_LIMITS(CUSTOMER_ID);
```

- [ ] **Step 3: Create V3__create_loans.sql**

```sql
CREATE TABLE LOANS (
    ID                    UUID          NOT NULL DEFAULT gen_random_uuid(),
    LOAN_NUMBER           VARCHAR(50)   NOT NULL,
    CUSTOMER_ID           UUID          NOT NULL,
    PRODUCT_ID            UUID          NOT NULL,
    PRINCIPAL_AMOUNT      NUMERIC(19,2) NOT NULL,
    DISBURSED_AMOUNT      NUMERIC(19,2),
    OUTSTANDING_BALANCE   NUMERIC(19,2) NOT NULL,
    STATUS                VARCHAR(15)   NOT NULL DEFAULT 'OPEN'
                                        CHECK (STATUS IN ('OPEN', 'CLOSED', 'CANCELLED', 'OVERDUE', 'WRITTEN_OFF')),
    LOAN_TYPE             VARCHAR(15)   NOT NULL
                                        CHECK (LOAN_TYPE IN ('LUMP_SUM', 'INSTALLMENT')),
    BILLING_CYCLE_TYPE    VARCHAR(15)   NOT NULL DEFAULT 'INDIVIDUAL'
                                        CHECK (BILLING_CYCLE_TYPE IN ('INDIVIDUAL', 'CONSOLIDATED')),
    CONSOLIDATED_DUE_DATE DATE,
    DUE_DATE              DATE          NOT NULL,
    DISBURSED_AT          TIMESTAMP,
    CLOSED_AT             TIMESTAMP,
    CREATED_AT            TIMESTAMP     NOT NULL DEFAULT NOW(),
    UPDATED_AT            TIMESTAMP     NOT NULL DEFAULT NOW(),
    CREATED_BY            VARCHAR(100),
    UPDATED_BY            VARCHAR(100),
    CONSTRAINT PK_LOANS PRIMARY KEY (ID),
    CONSTRAINT UQ_LOANS_NUMBER UNIQUE (LOAN_NUMBER),
    CONSTRAINT FK_LOANS_CUSTOMER FOREIGN KEY (CUSTOMER_ID) REFERENCES CUSTOMERS(ID),
    CONSTRAINT FK_LOANS_PRODUCT FOREIGN KEY (PRODUCT_ID) REFERENCES LOAN_PRODUCTS(ID)
);

CREATE INDEX IDX_LOANS_CUSTOMER_ID ON LOANS(CUSTOMER_ID);
CREATE INDEX IDX_LOANS_STATUS ON LOANS(STATUS);
CREATE INDEX IDX_LOANS_DUE_DATE ON LOANS(DUE_DATE);

CREATE TABLE LOAN_INSTALLMENTS (
    ID                 UUID          NOT NULL DEFAULT gen_random_uuid(),
    LOAN_ID            UUID          NOT NULL,
    INSTALLMENT_NUMBER INTEGER       NOT NULL,
    PRINCIPAL_AMOUNT   NUMERIC(19,2) NOT NULL,
    FEE_AMOUNT         NUMERIC(19,2) NOT NULL DEFAULT 0,
    TOTAL_AMOUNT       NUMERIC(19,2) NOT NULL,
    OUTSTANDING_AMOUNT NUMERIC(19,2) NOT NULL,
    DUE_DATE           DATE          NOT NULL,
    PAID_AT            TIMESTAMP,
    STATUS             VARCHAR(10)   NOT NULL DEFAULT 'PENDING'
                                     CHECK (STATUS IN ('PENDING', 'PAID', 'OVERDUE')),
    CREATED_AT         TIMESTAMP     NOT NULL DEFAULT NOW(),
    UPDATED_AT         TIMESTAMP     NOT NULL DEFAULT NOW(),
    CREATED_BY         VARCHAR(100),
    UPDATED_BY         VARCHAR(100),
    CONSTRAINT PK_LOAN_INSTALLMENTS PRIMARY KEY (ID),
    CONSTRAINT FK_LI_LOAN FOREIGN KEY (LOAN_ID) REFERENCES LOANS(ID),
    CONSTRAINT UQ_LI_LOAN_NUMBER UNIQUE (LOAN_ID, INSTALLMENT_NUMBER)
);

CREATE INDEX IDX_LOAN_INSTALLMENTS_LOAN_ID ON LOAN_INSTALLMENTS(LOAN_ID);
CREATE INDEX IDX_LOAN_INSTALLMENTS_STATUS ON LOAN_INSTALLMENTS(STATUS);
```

- [ ] **Step 4: Create V4__create_repayments.sql**

```sql
CREATE TABLE REPAYMENTS (
    ID           UUID          NOT NULL DEFAULT gen_random_uuid(),
    LOAN_ID      UUID          NOT NULL,
    AMOUNT       NUMERIC(19,2) NOT NULL CHECK (AMOUNT > 0),
    REFERENCE    VARCHAR(100)  NOT NULL,
    CHANNEL      VARCHAR(10)   NOT NULL CHECK (CHANNEL IN ('MPESA', 'BANK', 'CASH')),
    PROCESSED_AT TIMESTAMP     NOT NULL DEFAULT NOW(),
    CREATED_AT   TIMESTAMP     NOT NULL DEFAULT NOW(),
    UPDATED_AT   TIMESTAMP     NOT NULL DEFAULT NOW(),
    CREATED_BY   VARCHAR(100),
    UPDATED_BY   VARCHAR(100),
    CONSTRAINT PK_REPAYMENTS PRIMARY KEY (ID),
    CONSTRAINT UQ_REPAYMENTS_REFERENCE UNIQUE (REFERENCE),
    CONSTRAINT FK_REPAYMENTS_LOAN FOREIGN KEY (LOAN_ID) REFERENCES LOANS(ID)
);

CREATE INDEX IDX_REPAYMENTS_LOAN_ID ON REPAYMENTS(LOAN_ID);
```

- [ ] **Step 5: Create V5__create_notifications.sql**

```sql
CREATE TABLE NOTIFICATION_TEMPLATES (
    ID            UUID         NOT NULL DEFAULT gen_random_uuid(),
    EVENT_TYPE    VARCHAR(50)  NOT NULL
                               CHECK (EVENT_TYPE IN ('LOAN_CREATED','DUE_REMINDER','REPAYMENT_ACK','OVERDUE_NOTICE')),
    CHANNEL       VARCHAR(10)  NOT NULL CHECK (CHANNEL IN ('EMAIL', 'SMS', 'PUSH')),
    SUBJECT       VARCHAR(200),
    BODY_TEMPLATE TEXT         NOT NULL,
    ACTIVE        BOOLEAN      NOT NULL DEFAULT TRUE,
    CREATED_AT    TIMESTAMP    NOT NULL DEFAULT NOW(),
    UPDATED_AT    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CREATED_BY    VARCHAR(100),
    UPDATED_BY    VARCHAR(100),
    CONSTRAINT PK_NOTIFICATION_TEMPLATES PRIMARY KEY (ID),
    CONSTRAINT UQ_NT_TYPE_CHANNEL UNIQUE (EVENT_TYPE, CHANNEL)
);

CREATE TABLE NOTIFICATION_RULES (
    ID               UUID        NOT NULL DEFAULT gen_random_uuid(),
    PRODUCT_ID       UUID,
    CUSTOMER_SEGMENT VARCHAR(50),
    EVENT_TYPE       VARCHAR(50) NOT NULL,
    CHANNEL          VARCHAR(10) NOT NULL CHECK (CHANNEL IN ('EMAIL', 'SMS', 'PUSH')),
    ENABLED          BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIORITY         INTEGER     NOT NULL DEFAULT 0,
    DELAY_MINUTES    INTEGER     NOT NULL DEFAULT 0,
    CREATED_AT       TIMESTAMP   NOT NULL DEFAULT NOW(),
    UPDATED_AT       TIMESTAMP   NOT NULL DEFAULT NOW(),
    CREATED_BY       VARCHAR(100),
    UPDATED_BY       VARCHAR(100),
    CONSTRAINT PK_NOTIFICATION_RULES PRIMARY KEY (ID),
    CONSTRAINT FK_NR_PRODUCT FOREIGN KEY (PRODUCT_ID) REFERENCES LOAN_PRODUCTS(ID)
);

CREATE INDEX IDX_NOTIFICATION_RULES_EVENT_TYPE ON NOTIFICATION_RULES(EVENT_TYPE);

CREATE TABLE NOTIFICATION_LOGS (
    ID            UUID         NOT NULL DEFAULT gen_random_uuid(),
    CUSTOMER_ID   UUID         NOT NULL,
    EVENT_TYPE    VARCHAR(50)  NOT NULL,
    CHANNEL       VARCHAR(10)  NOT NULL,
    RECIPIENT     VARCHAR(200) NOT NULL,
    STATUS        VARCHAR(10)  NOT NULL DEFAULT 'PENDING'
                               CHECK (STATUS IN ('SENT', 'FAILED', 'PENDING')),
    ERROR_MESSAGE TEXT,
    SENT_AT       TIMESTAMP,
    CREATED_AT    TIMESTAMP    NOT NULL DEFAULT NOW(),
    UPDATED_AT    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CREATED_BY    VARCHAR(100),
    UPDATED_BY    VARCHAR(100),
    CONSTRAINT PK_NOTIFICATION_LOGS PRIMARY KEY (ID),
    CONSTRAINT FK_NL_CUSTOMER FOREIGN KEY (CUSTOMER_ID) REFERENCES CUSTOMERS(ID)
);

CREATE INDEX IDX_NOTIFICATION_LOGS_CUSTOMER_ID ON NOTIFICATION_LOGS(CUSTOMER_ID);
```

- [ ] **Step 6: Create V6__create_auth.sql**

```sql
CREATE TABLE APP_USERS (
    ID         UUID         NOT NULL DEFAULT gen_random_uuid(),
    USERNAME   VARCHAR(100) NOT NULL,
    PASSWORD   VARCHAR(255) NOT NULL,
    EMAIL      VARCHAR(150) NOT NULL,
    ROLE       VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER'
                            CHECK (ROLE IN ('ADMIN', 'CUSTOMER')),
    ENABLED    BOOLEAN      NOT NULL DEFAULT TRUE,
    CUSTOMER_ID UUID,
    CREATED_AT TIMESTAMP    NOT NULL DEFAULT NOW(),
    UPDATED_AT TIMESTAMP    NOT NULL DEFAULT NOW(),
    CREATED_BY VARCHAR(100),
    UPDATED_BY VARCHAR(100),
    CONSTRAINT PK_APP_USERS PRIMARY KEY (ID),
    CONSTRAINT UQ_APP_USERS_USERNAME UNIQUE (USERNAME),
    CONSTRAINT UQ_APP_USERS_EMAIL UNIQUE (EMAIL),
    CONSTRAINT FK_APP_USERS_CUSTOMER FOREIGN KEY (CUSTOMER_ID) REFERENCES CUSTOMERS(ID)
);
```

- [ ] **Step 7: Create V99__seed_data.sql**

```sql
-- Seed loan products
INSERT INTO LOAN_PRODUCTS (ID, NAME, DESCRIPTION, TENURE_VALUE, TENURE_TYPE, MIN_AMOUNT, MAX_AMOUNT, GRACE_PERIOD_DAYS, ACTIVE)
VALUES
    ('a1000000-0000-0000-0000-000000000001', 'Personal Quick Loan', 'Short-term personal loan up to 30 days', 30, 'DAYS', 1000.00, 50000.00, 3, TRUE),
    ('a1000000-0000-0000-0000-000000000002', 'Business Term Loan', '12-month business financing', 12, 'MONTHS', 10000.00, 500000.00, 5, TRUE);

-- Seed product fees
INSERT INTO PRODUCT_FEES (ID, PRODUCT_ID, FEE_TYPE, CALCULATION_TYPE, AMOUNT, DAYS_AFTER_DUE, ACTIVE)
VALUES
    ('b1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'SERVICE_FEE', 'PERCENTAGE', 5.00, 0, TRUE),
    ('b1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000001', 'LATE_FEE', 'FIXED', 500.00, 3, TRUE),
    ('b1000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000002', 'SERVICE_FEE', 'PERCENTAGE', 3.00, 0, TRUE),
    ('b1000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000002', 'DAILY_FEE', 'PERCENTAGE', 0.10, 0, TRUE),
    ('b1000000-0000-0000-0000-000000000005', 'a1000000-0000-0000-0000-000000000002', 'LATE_FEE', 'PERCENTAGE', 2.00, 5, TRUE);

-- Seed customers
INSERT INTO CUSTOMERS (ID, FIRST_NAME, LAST_NAME, EMAIL, PHONE, NATIONAL_ID, STATUS)
VALUES
    ('c1000000-0000-0000-0000-000000000001', 'Alice', 'Wanjiku', 'alice.wanjiku@example.com', '+254700000001', '12345678', 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000002', 'Brian', 'Omondi', 'brian.omondi@example.com', '+254700000002', '23456789', 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000003', 'Carol', 'Njeri', 'carol.njeri@example.com', '+254700000003', '34567890', 'ACTIVE');

-- Seed customer loan limits
INSERT INTO CUSTOMER_LOAN_LIMITS (ID, CUSTOMER_ID, MIN_LIMIT, MAX_LIMIT, CURRENT_LIMIT, CREDIT_SCORE, LAST_REVIEWED_AT)
VALUES
    ('d1000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001', 1000.00, 50000.00, 50000.00, 750, NOW()),
    ('d1000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000002', 1000.00, 100000.00, 100000.00, 800, NOW()),
    ('d1000000-0000-0000-0000-000000000003', 'c1000000-0000-0000-0000-000000000003', 1000.00, 30000.00, 30000.00, 600, NOW());

-- Seed notification templates
INSERT INTO NOTIFICATION_TEMPLATES (ID, EVENT_TYPE, CHANNEL, SUBJECT, BODY_TEMPLATE, ACTIVE)
VALUES
    ('e1000000-0000-0000-0000-000000000001', 'LOAN_CREATED', 'EMAIL',
     'Your Tezza Loan {{loanNumber}} Has Been Disbursed',
     'Dear {{customerName}},\n\nYour loan {{loanNumber}} of KES {{amount}} has been disbursed successfully.\nDue date: {{dueDate}}.\n\nTezza Lending', TRUE),
    ('e1000000-0000-0000-0000-000000000002', 'LOAN_CREATED', 'SMS', NULL,
     'Tezza: Loan {{loanNumber}} of KES {{amount}} disbursed. Due {{dueDate}}.', TRUE),
    ('e1000000-0000-0000-0000-000000000003', 'DUE_REMINDER', 'EMAIL',
     'Payment Reminder: Loan {{loanNumber}} Due in 3 Days',
     'Dear {{customerName}},\n\nYour loan {{loanNumber}} payment of KES {{amount}} is due on {{dueDate}}.\n\nTezza Lending', TRUE),
    ('e1000000-0000-0000-0000-000000000004', 'DUE_REMINDER', 'SMS', NULL,
     'Tezza: Payment reminder - Loan {{loanNumber}} KES {{amount}} due {{dueDate}}.', TRUE),
    ('e1000000-0000-0000-0000-000000000005', 'REPAYMENT_ACK', 'EMAIL',
     'Payment Received — Loan {{loanNumber}}',
     'Dear {{customerName}},\n\nWe received your payment of KES {{amount}} for loan {{loanNumber}}.\nOutstanding balance: KES {{balance}}.\n\nTezza Lending', TRUE),
    ('e1000000-0000-0000-0000-000000000006', 'REPAYMENT_ACK', 'SMS', NULL,
     'Tezza: Payment KES {{amount}} received for loan {{loanNumber}}. Balance: KES {{balance}}.', TRUE),
    ('e1000000-0000-0000-0000-000000000007', 'OVERDUE_NOTICE', 'EMAIL',
     'OVERDUE: Loan {{loanNumber}} Requires Immediate Attention',
     'Dear {{customerName}},\n\nYour loan {{loanNumber}} is now OVERDUE. Outstanding balance: KES {{amount}}.\nLate fees are accruing. Please pay immediately.\n\nTezza Lending', TRUE),
    ('e1000000-0000-0000-0000-000000000008', 'OVERDUE_NOTICE', 'SMS', NULL,
     'Tezza URGENT: Loan {{loanNumber}} overdue. KES {{amount}} outstanding. Pay now.', TRUE);

-- Seed notification rules (all events enabled, EMAIL + SMS)
INSERT INTO NOTIFICATION_RULES (ID, PRODUCT_ID, CUSTOMER_SEGMENT, EVENT_TYPE, CHANNEL, ENABLED, PRIORITY, DELAY_MINUTES)
VALUES
    ('f1000000-0000-0000-0000-000000000001', NULL, NULL, 'LOAN_CREATED', 'EMAIL', TRUE, 1, 0),
    ('f1000000-0000-0000-0000-000000000002', NULL, NULL, 'LOAN_CREATED', 'SMS', TRUE, 2, 0),
    ('f1000000-0000-0000-0000-000000000003', NULL, NULL, 'DUE_REMINDER', 'EMAIL', TRUE, 1, 0),
    ('f1000000-0000-0000-0000-000000000004', NULL, NULL, 'DUE_REMINDER', 'SMS', TRUE, 2, 0),
    ('f1000000-0000-0000-0000-000000000005', NULL, NULL, 'REPAYMENT_ACK', 'EMAIL', TRUE, 1, 0),
    ('f1000000-0000-0000-0000-000000000006', NULL, NULL, 'REPAYMENT_ACK', 'SMS', TRUE, 2, 0),
    ('f1000000-0000-0000-0000-000000000007', NULL, NULL, 'OVERDUE_NOTICE', 'EMAIL', TRUE, 1, 0),
    ('f1000000-0000-0000-0000-000000000008', NULL, NULL, 'OVERDUE_NOTICE', 'SMS', TRUE, 2, 0);

-- Seed admin user (password: admin123 BCrypt encoded)
INSERT INTO APP_USERS (ID, USERNAME, PASSWORD, EMAIL, ROLE, ENABLED)
VALUES ('00000000-0000-0000-0000-000000000001', 'admin',
        '$2a$12$RIZBHUuQnNJNBJj8gfY4EOmxMVo0NJqkbnxL8X/yBnLQH0JpXDC1S',
        'admin@tezza.co.ke', 'ADMIN', TRUE);
```

- [ ] **Step 8: Run migrations via app start to verify**

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.flyway.validateOnMigrate=true" &
sleep 15
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
pkill -f "spring-boot:run"
```

Expected: `"status": "UP"`

- [ ] **Step 9: Commit**

```bash
git add src/main/resources/db/migration/
git commit -m "feat: add Flyway migrations and seed data"
```

---

## Task 3: Shared Module

**Files:**
- Create: `src/main/java/com/tezza/lending/shared/BaseAuditEntity.java`
- Create: `src/main/java/com/tezza/lending/shared/ApiResponse.java`
- Create: `src/main/java/com/tezza/lending/shared/exception/ResourceNotFoundException.java`
- Create: `src/main/java/com/tezza/lending/shared/exception/BusinessException.java`
- Create: `src/main/java/com/tezza/lending/shared/exception/GlobalExceptionHandler.java`
- Create: `src/main/java/com/tezza/lending/shared/config/JpaAuditingConfig.java`
- Create: `src/main/java/com/tezza/lending/shared/util/LoanNumberGenerator.java`
- Create: `src/main/java/com/tezza/lending/shared/package-info.java`

- [ ] **Step 1: Create package-info.java to mark shared as open module**

```java
@org.springframework.modulith.open
package com.tezza.lending.shared;
```

- [ ] **Step 2: Create BaseAuditEntity.java**

```java
package com.tezza.lending.shared;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseAuditEntity {

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "CREATED_BY", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;
}
```

- [ ] **Step 3: Create ApiResponse.java**

```java
package com.tezza.lending.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.UUID;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final String correlationId;
    private final T data;

    private ApiResponse(int status, String message, String correlationId, T data) {
        this.status = status;
        this.message = message;
        this.correlationId = correlationId;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "Success", UUID.randomUUID().toString(), data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, UUID.randomUUID().toString(), data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(1, message, UUID.randomUUID().toString(), null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(1, message, UUID.randomUUID().toString(), data);
    }
}
```

- [ ] **Step 4: Create ResourceNotFoundException.java**

```java
package com.tezza.lending.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found with id: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 5: Create BusinessException.java**

```java
package com.tezza.lending.shared.exception;

public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
```

- [ ] **Step 6: Create GlobalExceptionHandler.java**

```java
package com.tezza.lending.shared.exception;

import com.tezza.lending.shared.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
```

- [ ] **Step 7: Create JpaAuditingConfig.java**

```java
package com.tezza.lending.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of("system");
            }
            return Optional.of(auth.getName());
        };
    }
}
```

- [ ] **Step 8: Create LoanNumberGenerator.java**

```java
package com.tezza.lending.shared.util;

import java.time.Year;
import java.util.Random;

public final class LoanNumberGenerator {

    private static final Random RANDOM = new Random();

    private LoanNumberGenerator() {}

    public static String generate() {
        int year = Year.now().getValue();
        long suffix = (long) (RANDOM.nextDouble() * 100_000_000L);
        return String.format("TZ-%d-%08d", year, suffix);
    }
}
```

- [ ] **Step 9: Write unit test for LoanNumberGenerator**

Create `src/test/java/com/tezza/lending/shared/LoanNumberGeneratorTest.java`:

```java
package com.tezza.lending.shared;

import com.tezza.lending.shared.util.LoanNumberGenerator;
import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;

class LoanNumberGeneratorTest {

    @Test
    void generatesLoanNumberWithCorrectPrefix() {
        String number = LoanNumberGenerator.generate();
        assertThat(number).startsWith("TZ-" + Year.now().getValue() + "-");
    }

    @Test
    void generatedNumberHasEightDigitSuffix() {
        String number = LoanNumberGenerator.generate();
        String suffix = number.substring(number.lastIndexOf('-') + 1);
        assertThat(suffix).hasSize(8).matches("\\d{8}");
    }

    @Test
    void twoGeneratedNumbersAreDifferent() {
        String n1 = LoanNumberGenerator.generate();
        String n2 = LoanNumberGenerator.generate();
        // extremely low collision probability; good enough for smoke test
        assertThat(n1).isNotEqualTo(n2);
    }
}
```

- [ ] **Step 10: Run shared tests**

```bash
mvn test -pl . -Dtest="LoanNumberGeneratorTest" -q
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/tezza/lending/shared/ \
        src/test/java/com/tezza/lending/shared/
git commit -m "feat: add shared module — BaseAuditEntity, ApiResponse, exceptions, utilities"
```
