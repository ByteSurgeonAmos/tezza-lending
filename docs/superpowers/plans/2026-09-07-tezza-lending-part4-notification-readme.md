# Tezza Lending — Part 4: Notification Module, Auth Correction & README

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Event-driven notification pipeline (Kafka + email + stubs), correct OAuth2 to pure client_credentials microservice auth, and final README.

**Depends on:** Parts 1–3 complete

---

## CORRECTION: Auth Module — Replace User Auth with Client Credentials

The auth design must use **OAuth2 client_credentials** (microservice pattern), not user password auth.
No per-user login. Registered clients (admin portal, mobile app) authenticate via client_id + client_secret.

**Changes to Task 4 from Part 2:**

- [ ] **Auth Correction Step 1: Replace V6__create_auth.sql**

Drop the `APP_USERS` table migration. Spring Authorization Server's JDBC store manages its own schema.
Replace `V6__create_auth.sql` with:

```sql
-- Spring Authorization Server JDBC schema (from spring-authorization-server source)
-- Tables: OAUTH2_REGISTERED_CLIENT, OAUTH2_AUTHORIZATION, OAUTH2_AUTHORIZATION_CONSENT
-- These are auto-created by the AS JDBC initializer, but explicit migration is best practice.

CREATE TABLE IF NOT EXISTS OAUTH2_REGISTERED_CLIENT (
    ID                            VARCHAR(100) NOT NULL,
    CLIENT_ID                     VARCHAR(100) NOT NULL,
    CLIENT_ID_ISSUED_AT           TIMESTAMP DEFAULT NOW() NOT NULL,
    CLIENT_SECRET                 VARCHAR(200),
    CLIENT_SECRET_EXPIRES_AT      TIMESTAMP,
    CLIENT_NAME                   VARCHAR(200) NOT NULL,
    CLIENT_AUTHENTICATION_METHODS VARCHAR(1000) NOT NULL,
    AUTHORIZATION_GRANT_TYPES     VARCHAR(1000) NOT NULL,
    REDIRECT_URIS                 VARCHAR(1000),
    POST_LOGOUT_REDIRECT_URIS     VARCHAR(1000),
    SCOPES                        VARCHAR(1000) NOT NULL,
    CLIENT_SETTINGS               VARCHAR(2000) NOT NULL,
    TOKEN_SETTINGS                VARCHAR(2000) NOT NULL,
    CONSTRAINT PK_OAUTH2_REGISTERED_CLIENT PRIMARY KEY (ID)
);

CREATE TABLE IF NOT EXISTS OAUTH2_AUTHORIZATION (
    ID                            VARCHAR(100) NOT NULL,
    REGISTERED_CLIENT_ID          VARCHAR(100) NOT NULL,
    PRINCIPAL_NAME                VARCHAR(200) NOT NULL,
    AUTHORIZATION_GRANT_TYPE      VARCHAR(100) NOT NULL,
    AUTHORIZED_SCOPES             VARCHAR(1000),
    ATTRIBUTES                    TEXT,
    STATE                         VARCHAR(500),
    AUTHORIZATION_CODE_VALUE      TEXT,
    AUTHORIZATION_CODE_ISSUED_AT  TIMESTAMP,
    AUTHORIZATION_CODE_EXPIRES_AT TIMESTAMP,
    AUTHORIZATION_CODE_METADATA   TEXT,
    ACCESS_TOKEN_VALUE            TEXT,
    ACCESS_TOKEN_ISSUED_AT        TIMESTAMP,
    ACCESS_TOKEN_EXPIRES_AT       TIMESTAMP,
    ACCESS_TOKEN_METADATA         TEXT,
    ACCESS_TOKEN_TYPE             VARCHAR(100),
    ACCESS_TOKEN_SCOPES           VARCHAR(1000),
    OID_TOKEN_VALUE               TEXT,
    OID_TOKEN_ISSUED_AT           TIMESTAMP,
    OID_TOKEN_EXPIRES_AT          TIMESTAMP,
    OID_TOKEN_CLAIMS              TEXT,
    REFRESH_TOKEN_VALUE           TEXT,
    REFRESH_TOKEN_ISSUED_AT       TIMESTAMP,
    REFRESH_TOKEN_EXPIRES_AT      TIMESTAMP,
    REFRESH_TOKEN_METADATA        TEXT,
    USER_CODE_VALUE               TEXT,
    USER_CODE_ISSUED_AT           TIMESTAMP,
    USER_CODE_EXPIRES_AT          TIMESTAMP,
    DEVICE_CODE_VALUE             TEXT,
    DEVICE_CODE_ISSUED_AT         TIMESTAMP,
    DEVICE_CODE_EXPIRES_AT        TIMESTAMP,
    DEVICE_CODE_METADATA          TEXT,
    CONSTRAINT PK_OAUTH2_AUTHORIZATION PRIMARY KEY (ID)
);

CREATE TABLE IF NOT EXISTS OAUTH2_AUTHORIZATION_CONSENT (
    REGISTERED_CLIENT_ID VARCHAR(100) NOT NULL,
    PRINCIPAL_NAME       VARCHAR(200) NOT NULL,
    AUTHORITIES          VARCHAR(1000) NOT NULL,
    CONSTRAINT PK_OAUTH2_AUTHORIZATION_CONSENT PRIMARY KEY (REGISTERED_CLIENT_ID, PRINCIPAL_NAME)
);
```

- [ ] **Auth Correction Step 2: Replace AuthorizationServerConfig.java**

Replace the previous `AuthorizationServerConfig.java` with JDBC-backed client store + correct scopes:

```java
package com.tezza.lending.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import javax.sql.DataSource;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());
        http.exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        http.oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * JDBC-backed client repository — persists registered clients in OAUTH2_REGISTERED_CLIENT table.
     * On startup, seeds default clients if absent (idempotent check via findByClientId).
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(DataSource dataSource, PasswordEncoder encoder) {
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(
                new org.springframework.jdbc.core.JdbcTemplate(dataSource));

        // Seed admin client if not already registered
        if (repository.findByClientId("tezza-admin-client") == null) {
            RegisteredClient adminClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("tezza-admin-client")
                    .clientSecret(encoder.encode("admin-secret"))
                    .clientName("Tezza Admin Portal")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scope("ADMIN")
                    .scope("CUSTOMER")
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofHours(1))
                            .build())
                    .build();
            repository.save(adminClient);
        }

        // Seed mobile/customer client
        if (repository.findByClientId("tezza-mobile-client") == null) {
            RegisteredClient mobileClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("tezza-mobile-client")
                    .clientSecret(encoder.encode("mobile-secret"))
                    .clientName("Tezza Mobile App")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scope("CUSTOMER")
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofHours(2))
                            .build())
                    .build();
            repository.save(mobileClient);
        }

        return repository;
    }

    /**
     * Customizes JWT to include scopes as ROLE_ authorities for @PreAuthorize compatibility.
     * Adds claim: "roles": ["ROLE_ADMIN", "ROLE_CUSTOMER"] derived from granted scopes.
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            if (context.getTokenType().getValue().equals("access_token")) {
                JwtClaimsSet.Builder claims = context.getClaims();
                var scopes = context.getAuthorizedScopes();
                // Map scopes to Spring Security roles
                var roles = scopes.stream()
                        .map(scope -> "ROLE_" + scope.toUpperCase())
                        .toList();
                claims.claim("roles", roles);
                claims.claim("client_id", context.getRegisteredClient().getClientId());
            }
        };
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8080")
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

- [ ] **Auth Correction Step 3: Replace SecurityConfig.java to extract roles from JWT claims**

```java
package com.tezza.lending.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/auth/clients/register",
            "/oauth2/token",
            "/oauth2/jwks",
            "/.well-known/openid-configuration",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health"
    };

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_PATHS).permitAll()
                    .anyRequest().authenticated())
            .oauth2ResourceServer(rs -> rs.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /**
     * Extracts "roles" claim from JWT (set by jwtTokenCustomizer) and converts to Spring authorities.
     * Enables @PreAuthorize("hasRole('ADMIN')") on controller methods.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RolesClaimConverter());
        return converter;
    }

    static class RolesClaimConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) return List.of();
            return roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
    }
}
```

- [ ] **Auth Correction Step 4: Update AuthController to register OAuth2 clients**

Replace previous AuthController with client registration endpoint:

```java
package com.tezza.lending.auth.web;

import com.tezza.lending.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/auth/clients")
@RequiredArgsConstructor
@Tag(name = "OAuth2 Client Registration")
public class AuthController {

    private final RegisteredClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Data
    public static class ClientRegistrationRequest {
        @NotBlank @Size(max = 100)
        private String clientId;

        @NotBlank @Size(min = 16)
        private String clientSecret;

        @NotBlank @Size(max = 200)
        private String clientName;

        @Pattern(regexp = "ADMIN|CUSTOMER", message = "scope must be ADMIN or CUSTOMER")
        private String scope = "CUSTOMER";
    }

    @Data
    public static class ClientRegistrationResponse {
        private String clientId;
        private String clientName;
        private String scope;
        private String tokenEndpoint;

        ClientRegistrationResponse(String clientId, String clientName, String scope) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.scope = scope;
            this.tokenEndpoint = "POST /oauth2/token (Basic auth: clientId:clientSecret, grant_type=client_credentials)";
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new OAuth2 client (microservice/app)")
    public ResponseEntity<ApiResponse<ClientRegistrationResponse>> register(
            @Valid @RequestBody ClientRegistrationRequest request) {

        if (clientRepository.findByClientId(request.getClientId()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Client ID already registered: " + request.getClientId()));
        }

        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(request.getClientId())
                .clientSecret(passwordEncoder.encode(request.getClientSecret()))
                .clientName(request.getClientName())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(request.getScope())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .build())
                .build();

        clientRepository.save(client);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Client registered",
                        new ClientRegistrationResponse(request.getClientId(), request.getClientName(), request.getScope())));
    }
}
```

- [ ] **Auth Correction Step 5: Remove AppUser entity and repository**

Delete these files (they are replaced by the JDBC OAuth2 client store):
- `src/main/java/com/tezza/lending/auth/internal/entity/AppUser.java`
- `src/main/java/com/tezza/lending/auth/internal/repository/AppUserRepository.java`
- `src/main/java/com/tezza/lending/auth/internal/service/AppUserDetailsService.java`
- `src/main/java/com/tezza/lending/auth/api/dto/RegisterRequest.java`

- [ ] **Auth Correction Step 6: Verify token flow**

```bash
# Get token for admin client
curl -s -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "tezza-admin-client:admin-secret" \
  -d "grant_type=client_credentials&scope=ADMIN" | python3 -m json.tool
```

Expected: `{ "access_token": "eyJ...", "token_type": "Bearer", "expires_in": 3600 }`

```bash
# Use token to call admin endpoint (replace TOKEN with actual value)
curl -s http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer TOKEN" | python3 -m json.tool
```

Expected: `{ "status": 0, "data": { "content": [...] } }`

- [ ] **Auth Correction Step 7: Commit**

```bash
git add src/main/java/com/tezza/lending/auth/
git commit -m "fix: replace user auth with OAuth2 client_credentials microservice pattern"
```

---

## Task 10: Notification Module

**Files:**
- Create: `src/main/java/com/tezza/lending/notification/internal/entity/NotificationTemplate.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/entity/NotificationRule.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/entity/NotificationLog.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/entity/enums/EventType.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/entity/enums/NotificationChannel.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/entity/enums/NotificationStatus.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/repository/NotificationTemplateRepository.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/repository/NotificationRuleRepository.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/repository/NotificationLogRepository.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/service/TemplateRendererService.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/service/NotificationDispatchService.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/kafka/LoanEventMessage.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/kafka/KafkaConfig.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/kafka/LoanEventProducer.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/kafka/LoanEventConsumer.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/listener/LoanEventListener.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/dispatcher/EmailDispatcher.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/dispatcher/SmsDispatcher.java`
- Create: `src/main/java/com/tezza/lending/notification/internal/dispatcher/PushDispatcher.java`
- Create: `src/main/java/com/tezza/lending/notification/api/NotificationService.java`
- Create: `src/main/java/com/tezza/lending/notification/api/dto/TemplateRequest.java`
- Create: `src/main/java/com/tezza/lending/notification/api/dto/TemplateResponse.java`
- Create: `src/main/java/com/tezza/lending/notification/api/dto/NotificationRuleRequest.java`
- Create: `src/main/java/com/tezza/lending/notification/api/dto/NotificationLogResponse.java`
- Create: `src/main/java/com/tezza/lending/notification/web/NotificationController.java`
- Test: `src/test/java/com/tezza/lending/notification/TemplateRendererServiceTest.java`
- Test: `src/test/java/com/tezza/lending/notification/EmailDispatcherTest.java`
- Test: `src/test/java/com/tezza/lending/notification/LoanEventConsumerTest.java`

- [ ] **Step 1: Create enums**

```java
// EventType.java
package com.tezza.lending.notification.internal.entity.enums;
public enum EventType { LOAN_CREATED, DUE_REMINDER, REPAYMENT_ACK, OVERDUE_NOTICE }

// NotificationChannel.java
package com.tezza.lending.notification.internal.entity.enums;
public enum NotificationChannel { EMAIL, SMS, PUSH }

// NotificationStatus.java
package com.tezza.lending.notification.internal.entity.enums;
public enum NotificationStatus { SENT, FAILED, PENDING }
```

- [ ] **Step 2: Create entities**

```java
// NotificationTemplate.java
package com.tezza.lending.notification.internal.entity;

import com.tezza.lending.notification.internal.entity.enums.EventType;
import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "NOTIFICATION_TEMPLATES")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationTemplate extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "CHANNEL", nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(name = "SUBJECT", length = 200)
    private String subject;

    @Column(name = "BODY_TEMPLATE", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(name = "ACTIVE", nullable = false)
    @Builder.Default
    private boolean active = true;
}
```

```java
// NotificationRule.java
package com.tezza.lending.notification.internal.entity;

import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "NOTIFICATION_RULES")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationRule extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "PRODUCT_ID")
    private UUID productId; // null = applies to all products

    @Column(name = "CUSTOMER_SEGMENT", length = 50)
    private String customerSegment; // null = applies to all segments

    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "CHANNEL", nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(name = "ENABLED", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "PRIORITY", nullable = false)
    @Builder.Default
    private int priority = 0;

    @Column(name = "DELAY_MINUTES", nullable = false)
    @Builder.Default
    private int delayMinutes = 0;
}
```

```java
// NotificationLog.java
package com.tezza.lending.notification.internal.entity;

import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import com.tezza.lending.notification.internal.entity.enums.NotificationStatus;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "NOTIFICATION_LOGS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationLog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private UUID customerId;

    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "CHANNEL", nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(name = "RECIPIENT", nullable = false, length = 200)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 10)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "ERROR_MESSAGE", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "SENT_AT")
    private LocalDateTime sentAt;
}
```

- [ ] **Step 3: Create repositories**

```java
// NotificationTemplateRepository.java
package com.tezza.lending.notification.internal.repository;

import com.tezza.lending.notification.internal.entity.NotificationTemplate;
import com.tezza.lending.notification.internal.entity.enums.EventType;
import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    Optional<NotificationTemplate> findByEventTypeAndChannelAndActive(
            EventType eventType, NotificationChannel channel, boolean active);
    List<NotificationTemplate> findByActive(boolean active);
}
```

```java
// NotificationRuleRepository.java
package com.tezza.lending.notification.internal.repository;

import com.tezza.lending.notification.internal.entity.NotificationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRuleRepository extends JpaRepository<NotificationRule, UUID> {
    List<NotificationRule> findByEventTypeAndEnabledOrderByPriorityAsc(String eventType, boolean enabled);
}
```

```java
// NotificationLogRepository.java
package com.tezza.lending.notification.internal.repository;

import com.tezza.lending.notification.internal.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    List<NotificationLog> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
```

- [ ] **Step 4: Create TemplateRendererService**

```java
package com.tezza.lending.notification.internal.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateRendererService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    /**
     * Replaces all {{key}} placeholders in template with values from vars map.
     * Unmatched placeholders are left as-is.
     */
    public String render(String template, Map<String, String> vars) {
        if (template == null || template.isBlank()) return template == null ? "" : template;
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = vars.getOrDefault(key, matcher.group(0)); // leave as-is if missing
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
```

- [ ] **Step 5: Write TemplateRendererService tests first**

Create `src/test/java/com/tezza/lending/notification/TemplateRendererServiceTest.java`:

```java
package com.tezza.lending.notification;

import com.tezza.lending.notification.internal.service.TemplateRendererService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererServiceTest {

    private final TemplateRendererService renderer = new TemplateRendererService();

    @Test
    void render_singleVariable_substituted() {
        String result = renderer.render("Hello {{name}}!", Map.of("name", "Alice"));
        assertThat(result).isEqualTo("Hello Alice!");
    }

    @Test
    void render_multipleVariables_allSubstituted() {
        String template = "Loan {{loanNumber}} of KES {{amount}} due {{dueDate}}";
        Map<String, String> vars = Map.of(
                "loanNumber", "TZ-2026-00000001",
                "amount", "10,000",
                "dueDate", "2026-10-07");
        String result = renderer.render(template, vars);
        assertThat(result).isEqualTo("Loan TZ-2026-00000001 of KES 10,000 due 2026-10-07");
    }

    @Test
    void render_missingVariable_leftAsIs() {
        String result = renderer.render("Hello {{name}} and {{unknown}}!", Map.of("name", "Bob"));
        assertThat(result).isEqualTo("Hello Bob and {{unknown}}!");
    }

    @Test
    void render_emptyTemplate_returnsEmpty() {
        assertThat(renderer.render("", Map.of("key", "value"))).isEmpty();
    }

    @Test
    void render_nullTemplate_returnsEmpty() {
        assertThat(renderer.render(null, Map.of())).isEmpty();
    }

    @Test
    void render_noPlaceholders_returnsTemplateUnchanged() {
        String template = "No placeholders here.";
        assertThat(renderer.render(template, Map.of("key", "value"))).isEqualTo(template);
    }
}
```

- [ ] **Step 6: Run template renderer tests**

```bash
mvn test -Dtest="TemplateRendererServiceTest" -q
```

Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 7: Create LoanEventMessage (Kafka DTO)**

```java
package com.tezza.lending.notification.internal.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanEventMessage {
    private String eventType;          // matches EventType enum name
    private UUID loanId;
    private UUID customerId;
    private String customerEmail;
    private String customerPhone;
    private String customerName;
    private String loanNumber;
    private BigDecimal amount;
    private BigDecimal outstandingBalance;
    private LocalDate dueDate;
}
```

- [ ] **Step 8: Create KafkaConfig**

```java
package com.tezza.lending.notification.internal.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_NOTIFICATIONS = "lending.notifications";
    public static final String TOPIC_LOAN_EVENTS = "lending.loan-events";

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(TOPIC_NOTIFICATIONS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic loanEventsTopic() {
        return TopicBuilder.name(TOPIC_LOAN_EVENTS).partitions(3).replicas(1).build();
    }
}
```

- [ ] **Step 9: Create LoanEventProducer**

```java
package com.tezza.lending.notification.internal.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanEventProducer {

    private final KafkaTemplate<String, LoanEventMessage> kafkaTemplate;

    public void send(LoanEventMessage message) {
        kafkaTemplate.send(KafkaConfig.TOPIC_NOTIFICATIONS, message.getLoanId().toString(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send notification event for loan {}: {}", message.getLoanNumber(), ex.getMessage());
                    } else {
                        log.debug("Notification event sent for loan {} event {}", message.getLoanNumber(), message.getEventType());
                    }
                });
    }
}
```

- [ ] **Step 10: Create dispatchers**

```java
// EmailDispatcher.java
package com.tezza.lending.notification.internal.dispatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailDispatcher {

    private final JavaMailSender mailSender;

    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject != null ? subject : "Tezza Lending Notification");
            message.setText(body);
            message.setFrom("noreply@tezza.co.ke");
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw e;
        }
    }
}
```

```java
// SmsDispatcher.java
package com.tezza.lending.notification.internal.dispatcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsDispatcher {

    public void send(String phoneNumber, String body) {
        // STUB: log instead of real SMS. Replace with Africa's Talking / Twilio / etc.
        log.info("[SMS STUB] To: {} | Message: {}", phoneNumber, body);
    }
}
```

```java
// PushDispatcher.java
package com.tezza.lending.notification.internal.dispatcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class PushDispatcher {

    public void send(UUID customerId, String title, String body) {
        // STUB: log instead of real push. Replace with Firebase / OneSignal / etc.
        log.info("[PUSH STUB] CustomerId: {} | Title: {} | Body: {}", customerId, title, body);
    }
}
```

- [ ] **Step 11: Create NotificationDispatchService**

```java
package com.tezza.lending.notification.internal.service;

import com.tezza.lending.notification.internal.dispatcher.EmailDispatcher;
import com.tezza.lending.notification.internal.dispatcher.PushDispatcher;
import com.tezza.lending.notification.internal.dispatcher.SmsDispatcher;
import com.tezza.lending.notification.internal.entity.NotificationLog;
import com.tezza.lending.notification.internal.entity.NotificationRule;
import com.tezza.lending.notification.internal.entity.NotificationTemplate;
import com.tezza.lending.notification.internal.entity.enums.EventType;
import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import com.tezza.lending.notification.internal.entity.enums.NotificationStatus;
import com.tezza.lending.notification.internal.kafka.LoanEventMessage;
import com.tezza.lending.notification.internal.repository.NotificationLogRepository;
import com.tezza.lending.notification.internal.repository.NotificationRuleRepository;
import com.tezza.lending.notification.internal.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    private final NotificationRuleRepository ruleRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final TemplateRendererService templateRenderer;
    private final EmailDispatcher emailDispatcher;
    private final SmsDispatcher smsDispatcher;
    private final PushDispatcher pushDispatcher;

    @Transactional
    public void dispatch(LoanEventMessage message) {
        EventType eventType;
        try {
            eventType = EventType.valueOf(message.getEventType());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown event type: {}", message.getEventType());
            return;
        }

        List<NotificationRule> rules = ruleRepository.findByEventTypeAndEnabledOrderByPriorityAsc(
                message.getEventType(), true);

        Map<String, String> vars = buildVariables(message);

        for (NotificationRule rule : rules) {
            // Skip rules scoped to a specific product that doesn't match
            if (rule.getProductId() != null) continue; // simplified: only global rules

            Optional<NotificationTemplate> templateOpt = templateRepository
                    .findByEventTypeAndChannelAndActive(eventType, rule.getChannel(), true);

            if (templateOpt.isEmpty()) {
                log.debug("No active template for event {} channel {}", eventType, rule.getChannel());
                continue;
            }

            NotificationTemplate template = templateOpt.get();
            String renderedBody = templateRenderer.render(template.getBodyTemplate(), vars);
            String renderedSubject = templateRenderer.render(
                    template.getSubject() != null ? template.getSubject() : "", vars);

            NotificationLog notifLog = NotificationLog.builder()
                    .customerId(message.getCustomerId())
                    .eventType(message.getEventType())
                    .channel(rule.getChannel())
                    .recipient(resolveRecipient(rule.getChannel(), message))
                    .status(NotificationStatus.PENDING)
                    .build();

            try {
                sendViaChannel(rule.getChannel(), message, renderedSubject, renderedBody);
                notifLog.setStatus(NotificationStatus.SENT);
                notifLog.setSentAt(LocalDateTime.now());
                log.info("Notification sent: {} via {} to {}", eventType, rule.getChannel(), notifLog.getRecipient());
            } catch (Exception e) {
                notifLog.setStatus(NotificationStatus.FAILED);
                notifLog.setErrorMessage(e.getMessage());
                log.error("Notification failed: {} via {} — {}", eventType, rule.getChannel(), e.getMessage());
            }
            logRepository.save(notifLog);
        }
    }

    private void sendViaChannel(NotificationChannel channel, LoanEventMessage message,
                                 String subject, String body) {
        switch (channel) {
            case EMAIL -> emailDispatcher.send(message.getCustomerEmail(), subject, body);
            case SMS -> smsDispatcher.send(message.getCustomerPhone(), body);
            case PUSH -> pushDispatcher.send(message.getCustomerId(), subject, body);
        }
    }

    private String resolveRecipient(NotificationChannel channel, LoanEventMessage message) {
        return switch (channel) {
            case EMAIL -> message.getCustomerEmail() != null ? message.getCustomerEmail() : "unknown";
            case SMS -> message.getCustomerPhone() != null ? message.getCustomerPhone() : "unknown";
            case PUSH -> message.getCustomerId().toString();
        };
    }

    private Map<String, String> buildVariables(LoanEventMessage message) {
        return Map.of(
                "customerName", message.getCustomerName() != null ? message.getCustomerName() : "",
                "loanNumber", message.getLoanNumber() != null ? message.getLoanNumber() : "",
                "amount", message.getAmount() != null ? message.getAmount().toPlainString() : "0",
                "balance", message.getOutstandingBalance() != null ? message.getOutstandingBalance().toPlainString() : "0",
                "dueDate", message.getDueDate() != null ? message.getDueDate().toString() : ""
        );
    }
}
```

- [ ] **Step 12: Create LoanEventConsumer**

```java
package com.tezza.lending.notification.internal.kafka;

import com.tezza.lending.notification.internal.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanEventConsumer {

    private final NotificationDispatchService dispatchService;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_NOTIFICATIONS,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(LoanEventMessage message) {
        log.info("Received notification event: {} for loan {}", message.getEventType(), message.getLoanNumber());
        try {
            dispatchService.dispatch(message);
        } catch (Exception e) {
            log.error("Error processing notification event {}: {}", message.getEventType(), e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 13: Create LoanEventListener (Spring → Kafka bridge)**

```java
package com.tezza.lending.notification.internal.listener;

import com.tezza.lending.customer.internal.entity.Customer;
import com.tezza.lending.customer.internal.repository.CustomerRepository;
import com.tezza.lending.loan.api.event.DueDateReminderEvent;
import com.tezza.lending.loan.api.event.LoanCreatedEvent;
import com.tezza.lending.loan.api.event.LoanOverdueEvent;
import com.tezza.lending.loan.api.event.RepaymentReceivedEvent;
import com.tezza.lending.notification.internal.kafka.LoanEventMessage;
import com.tezza.lending.notification.internal.kafka.LoanEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanEventListener {

    private final LoanEventProducer producer;
    private final CustomerRepository customerRepository;

    @EventListener
    @Async
    public void onLoanCreated(LoanCreatedEvent event) {
        Customer customer = findCustomer(event.customerId());
        LoanEventMessage message = LoanEventMessage.builder()
                .eventType("LOAN_CREATED")
                .loanId(event.loanId())
                .customerId(event.customerId())
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .customerName(customer != null ? customer.getFirstName() + " " + customer.getLastName() : null)
                .loanNumber(event.loanNumber())
                .amount(event.amount())
                .dueDate(event.dueDate())
                .build();
        producer.send(message);
    }

    @EventListener
    @Async
    public void onLoanOverdue(LoanOverdueEvent event) {
        Customer customer = findCustomer(event.customerId());
        LoanEventMessage message = LoanEventMessage.builder()
                .eventType("OVERDUE_NOTICE")
                .loanId(event.loanId())
                .customerId(event.customerId())
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .customerName(customer != null ? customer.getFirstName() + " " + customer.getLastName() : null)
                .loanNumber(event.loanNumber())
                .amount(event.outstandingBalance())
                .outstandingBalance(event.outstandingBalance())
                .build();
        producer.send(message);
    }

    @EventListener
    @Async
    public void onDueDateReminder(DueDateReminderEvent event) {
        Customer customer = findCustomer(event.customerId());
        LoanEventMessage message = LoanEventMessage.builder()
                .eventType("DUE_REMINDER")
                .loanId(event.loanId())
                .customerId(event.customerId())
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .customerName(customer != null ? customer.getFirstName() + " " + customer.getLastName() : null)
                .loanNumber(event.loanNumber())
                .amount(event.outstandingBalance())
                .outstandingBalance(event.outstandingBalance())
                .dueDate(event.dueDate())
                .build();
        producer.send(message);
    }

    @EventListener
    @Async
    public void onRepaymentReceived(RepaymentReceivedEvent event) {
        Customer customer = findCustomer(event.customerId());
        LoanEventMessage message = LoanEventMessage.builder()
                .eventType("REPAYMENT_ACK")
                .loanId(event.loanId())
                .customerId(event.customerId())
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .customerName(customer != null ? customer.getFirstName() + " " + customer.getLastName() : null)
                .loanNumber(event.loanNumber())
                .amount(event.amountPaid())
                .outstandingBalance(event.remainingBalance())
                .build();
        producer.send(message);
    }

    private Customer findCustomer(java.util.UUID customerId) {
        return customerRepository.findById(customerId).orElse(null);
    }
}
```

- [ ] **Step 14: Create DTOs and NotificationService interface**

```java
// TemplateRequest.java
package com.tezza.lending.notification.api.dto;

import com.tezza.lending.notification.internal.entity.enums.EventType;
import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TemplateRequest {
    @NotNull EventType eventType;
    @NotNull NotificationChannel channel;
    String subject;
    @NotBlank String bodyTemplate;
}
```

```java
// TemplateResponse.java
package com.tezza.lending.notification.api.dto;

import com.tezza.lending.notification.internal.entity.NotificationTemplate;
import com.tezza.lending.notification.internal.entity.enums.EventType;
import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import lombok.Data;
import java.util.UUID;

@Data
public class TemplateResponse {
    private UUID id;
    private EventType eventType;
    private NotificationChannel channel;
    private String subject;
    private String bodyTemplate;
    private boolean active;

    public static TemplateResponse from(NotificationTemplate t) {
        TemplateResponse r = new TemplateResponse();
        r.setId(t.getId());
        r.setEventType(t.getEventType());
        r.setChannel(t.getChannel());
        r.setSubject(t.getSubject());
        r.setBodyTemplate(t.getBodyTemplate());
        r.setActive(t.isActive());
        return r;
    }
}
```

```java
// NotificationRuleRequest.java
package com.tezza.lending.notification.api.dto;

import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class NotificationRuleRequest {
    UUID productId;
    String customerSegment;
    @NotBlank String eventType;
    @NotNull NotificationChannel channel;
    boolean enabled = true;
    @Min(0) int priority = 0;
    @Min(0) int delayMinutes = 0;
}
```

```java
// NotificationLogResponse.java
package com.tezza.lending.notification.api.dto;

import com.tezza.lending.notification.internal.entity.NotificationLog;
import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import com.tezza.lending.notification.internal.entity.enums.NotificationStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NotificationLogResponse {
    private UUID id;
    private UUID customerId;
    private String eventType;
    private NotificationChannel channel;
    private String recipient;
    private NotificationStatus status;
    private String errorMessage;
    private LocalDateTime sentAt;

    public static NotificationLogResponse from(NotificationLog l) {
        NotificationLogResponse r = new NotificationLogResponse();
        r.setId(l.getId());
        r.setCustomerId(l.getCustomerId());
        r.setEventType(l.getEventType());
        r.setChannel(l.getChannel());
        r.setRecipient(l.getRecipient());
        r.setStatus(l.getStatus());
        r.setErrorMessage(l.getErrorMessage());
        r.setSentAt(l.getSentAt());
        return r;
    }
}
```

```java
// NotificationService.java
package com.tezza.lending.notification.api;

import com.tezza.lending.notification.api.dto.*;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    TemplateResponse createTemplate(TemplateRequest request);
    List<TemplateResponse> listTemplates();
    TemplateResponse updateTemplate(UUID id, TemplateRequest request);
    NotificationRuleRequest createRule(NotificationRuleRequest request);
    List<NotificationLogResponse> getCustomerLogs(UUID customerId);
}
```

- [ ] **Step 15: Create NotificationController**

```java
package com.tezza.lending.notification.web;

import com.tezza.lending.notification.api.NotificationService;
import com.tezza.lending.notification.api.dto.*;
import com.tezza.lending.notification.internal.entity.NotificationRule;
import com.tezza.lending.notification.internal.entity.NotificationTemplate;
import com.tezza.lending.notification.internal.repository.NotificationLogRepository;
import com.tezza.lending.notification.internal.repository.NotificationRuleRepository;
import com.tezza.lending.notification.internal.repository.NotificationTemplateRepository;
import com.tezza.lending.shared.ApiResponse;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationController {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationRuleRepository ruleRepository;
    private final NotificationLogRepository logRepository;

    @PostMapping("/templates")
    @Operation(summary = "Create a notification template")
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @Valid @RequestBody TemplateRequest request) {
        NotificationTemplate template = NotificationTemplate.builder()
                .eventType(request.getEventType())
                .channel(request.getChannel())
                .subject(request.getSubject())
                .bodyTemplate(request.getBodyTemplate())
                .active(true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Template created", TemplateResponse.from(templateRepository.save(template))));
    }

    @GetMapping("/templates")
    @Operation(summary = "List all notification templates")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> listTemplates() {
        List<TemplateResponse> templates = templateRepository.findAll().stream()
                .map(TemplateResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @PutMapping("/templates/{id}")
    @Operation(summary = "Update a notification template")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @PathVariable UUID id, @Valid @RequestBody TemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", id.toString()));
        template.setSubject(request.getSubject());
        template.setBodyTemplate(request.getBodyTemplate());
        return ResponseEntity.ok(ApiResponse.success("Template updated", TemplateResponse.from(templateRepository.save(template))));
    }

    @PostMapping("/rules")
    @Operation(summary = "Create a notification rule")
    public ResponseEntity<ApiResponse<NotificationRule>> createRule(
            @Valid @RequestBody NotificationRuleRequest request) {
        NotificationRule rule = NotificationRule.builder()
                .productId(request.getProductId())
                .customerSegment(request.getCustomerSegment())
                .eventType(request.getEventType())
                .channel(request.getChannel())
                .enabled(request.isEnabled())
                .priority(request.getPriority())
                .delayMinutes(request.getDelayMinutes())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rule created", ruleRepository.save(rule)));
    }

    @GetMapping("/logs/{customerId}")
    @Operation(summary = "Get notification logs for a customer")
    public ResponseEntity<ApiResponse<List<NotificationLogResponse>>> getLogs(@PathVariable UUID customerId) {
        List<NotificationLogResponse> logs = logRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(NotificationLogResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
```

- [ ] **Step 16: Write EmailDispatcher test**

Create `src/test/java/com/tezza/lending/notification/EmailDispatcherTest.java`:

```java
package com.tezza.lending.notification;

import com.tezza.lending.notification.internal.dispatcher.EmailDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailDispatcherTest {

    @Mock JavaMailSender mailSender;
    @InjectMocks EmailDispatcher emailDispatcher;

    @Test
    void send_callsMailSenderWithCorrectFields() {
        emailDispatcher.send("alice@example.com", "Test Subject", "Test body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getTo()).containsExactly("alice@example.com");
        assertThat(sent.getSubject()).isEqualTo("Test Subject");
        assertThat(sent.getText()).isEqualTo("Test body");
        assertThat(sent.getFrom()).isEqualTo("noreply@tezza.co.ke");
    }

    @Test
    void send_nullSubject_usesDefaultSubject() {
        emailDispatcher.send("alice@example.com", null, "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("Tezza Lending Notification");
    }
}
```

- [ ] **Step 17: Run notification tests**

```bash
mvn test -Dtest="TemplateRendererServiceTest,EmailDispatcherTest" -q
```

Expected: `Tests run: 8, Failures: 0, Errors: 0`

- [ ] **Step 18: Commit**

```bash
git add src/main/java/com/tezza/lending/notification/ \
        src/test/java/com/tezza/lending/notification/
git commit -m "feat: add notification module — Kafka pipeline, email, SMS/push stubs, templates, rules"
```

---

## Task 11: Run Full Test Suite + Modularity Check

- [ ] **Step 1: Run all tests**

```bash
mvn test -q
```

Expected: All tests pass. Check output for any failures.

- [ ] **Step 2: Run modularity verification**

```bash
mvn test -Dtest="ModularityTest" -q
```

Expected: `Tests run: 2, Failures: 0, Errors: 0`
If modularity fails: check that no `internal` package is imported across modules. Only `api` subpackages should cross boundaries.

- [ ] **Step 3: Run JaCoCo coverage check**

```bash
mvn verify -q
```

Expected: BUILD SUCCESS (coverage >= 80% on service layer)
If coverage fails: add tests for uncovered service methods.

- [ ] **Step 4: Commit coverage reports** (do NOT commit target/ — only source)

```bash
# Already ignored via .gitignore — no action needed for target/
git status
```

---

## Task 12: README

**File:** `README.md`

- [ ] **Step 1: Create README.md**

```markdown
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
```

- [ ] **Step 2: Commit README**

```bash
git add README.md
git commit -m "docs: add comprehensive README with setup, architecture, API reference"
```

- [ ] **Step 3: Push all to GitHub**

```bash
git push origin main
```

Expected: All commits pushed, repo at https://github.com/ByteSurgeonAmos/tezza-lending

---

## Sample API Walkthrough (Verification)

- [ ] **Step 1: Start app and get admin token**

```bash
docker compose up -d
mvn spring-boot:run &
sleep 20

TOKEN=$(curl -s -X POST http://localhost:8080/oauth2/token \
  -u "tezza-admin-client:admin-secret" \
  -d "grant_type=client_credentials&scope=ADMIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
echo "Token obtained: ${TOKEN:0:20}..."
```

- [ ] **Step 2: Verify seed products load**

```bash
curl -s http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

Expected: 2 products in `data.content`

- [ ] **Step 3: Disburse a loan**

```bash
curl -s -X POST http://localhost:8080/api/v1/loans \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "c1000000-0000-0000-0000-000000000001",
    "productId": "a1000000-0000-0000-0000-000000000001",
    "amount": 10000,
    "loanType": "LUMP_SUM",
    "billingCycleType": "INDIVIDUAL"
  }' | python3 -m json.tool
```

Expected: loan with `"status": "OPEN"`, `"loanNumber": "TZ-2026-..."`, notification event queued to Kafka

- [ ] **Step 4: Process a repayment**

```bash
# Use the loanId from step 3
curl -s -X POST http://localhost:8080/api/v1/repayments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "loanId": "<LOAN_ID_FROM_STEP_3>",
    "amount": 5000,
    "reference": "MPESA-TEST-001",
    "channel": "MPESA"
  }' | python3 -m json.tool
```

Expected: repayment processed, loan outstanding balance reduced to ~5500 (10000 + 5% fee - 5000)
