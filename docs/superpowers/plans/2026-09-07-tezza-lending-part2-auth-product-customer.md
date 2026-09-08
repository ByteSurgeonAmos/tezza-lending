# Tezza Lending — Part 2: Auth, Product, Customer Modules

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement OAuth2 auth, loan product configuration with fees, and customer profile management.

**Depends on:** Part 1 complete (scaffold, migrations, shared module)

---

## Task 4: Auth Module

**Files:**
- Create: `src/main/java/com/tezza/lending/auth/internal/entity/AppUser.java`
- Create: `src/main/java/com/tezza/lending/auth/internal/repository/AppUserRepository.java`
- Create: `src/main/java/com/tezza/lending/auth/internal/service/AppUserDetailsService.java`
- Create: `src/main/java/com/tezza/lending/auth/api/dto/RegisterRequest.java`
- Create: `src/main/java/com/tezza/lending/auth/web/AuthController.java`
- Create: `src/main/java/com/tezza/lending/auth/AuthorizationServerConfig.java`
- Create: `src/main/java/com/tezza/lending/auth/SecurityConfig.java`

- [ ] **Step 1: Create AppUser entity**

```java
package com.tezza.lending.auth.internal.entity;

import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "APP_USERS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "USERNAME", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "ROLE", nullable = false, length = 20)
    private String role;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled = true;

    @Column(name = "CUSTOMER_ID")
    private UUID customerId;
}
```

- [ ] **Step 2: Create AppUserRepository**

```java
package com.tezza.lending.auth.internal.repository;

import com.tezza.lending.auth.internal.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 3: Create AppUserDetailsService**

```java
package com.tezza.lending.auth.internal.service;

import com.tezza.lending.auth.internal.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
                        .disabled(!user.isEnabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
```

- [ ] **Step 4: Create RegisterRequest DTO**

```java
package com.tezza.lending.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username required")
    @Size(min = 3, max = 100)
    private String username;

    @NotBlank(message = "Password required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank @Email(message = "Valid email required")
    private String email;

    // CUSTOMER by default; ADMIN only via seed data or direct DB
    private String role = "CUSTOMER";
}
```

- [ ] **Step 5: Create AuthController**

```java
package com.tezza.lending.auth.web;

import com.tezza.lending.auth.api.dto.RegisterRequest;
import com.tezza.lending.auth.internal.entity.AppUser;
import com.tezza.lending.auth.internal.repository.AppUserRepository;
import com.tezza.lending.shared.ApiResponse;
import com.tezza.lending.shared.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }

        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role("CUSTOMER") // force CUSTOMER; never trust client-supplied role
                .enabled(true)
                .build();

        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created. Use POST /oauth2/token to authenticate.", user.getUsername()));
    }
}
```

- [ ] **Step 6: Create AuthorizationServerConfig**

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
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

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

    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder encoder) {
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("tezza-client")
                .clientSecret(encoder.encode("tezza-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:8080/swagger-ui/oauth2-redirect.html")
                .scope(OidcScopes.OPENID)
                .scope("read")
                .scope("write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .build())
                .build();
        return new InMemoryRegisteredClientRepository(client);
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

- [ ] **Step 7: Create SecurityConfig**

```java
package com.tezza.lending.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/auth/register",
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
            .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))
            .formLogin(Customizer.withDefaults()); // needed for authorization_code flow
        return http.build();
    }
}
```

- [ ] **Step 8: Verify auth endpoints start**

```bash
mvn spring-boot:run &
sleep 20
curl -s http://localhost:8080/actuator/health
curl -s -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"pass123","email":"test@tezza.co.ke"}' | python3 -m json.tool
pkill -f "spring-boot:run"
```

Expected: register returns `"status": 0`

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/tezza/lending/auth/
git commit -m "feat: add auth module — Spring Authorization Server, OAuth2, user registration"
```

---

## Task 5: Product Module

**Files:**
- Create: `src/main/java/com/tezza/lending/product/internal/entity/LoanProduct.java`
- Create: `src/main/java/com/tezza/lending/product/internal/entity/ProductFee.java`
- Create: `src/main/java/com/tezza/lending/product/internal/entity/enums/TenureType.java`
- Create: `src/main/java/com/tezza/lending/product/internal/entity/enums/FeeType.java`
- Create: `src/main/java/com/tezza/lending/product/internal/entity/enums/CalculationType.java`
- Create: `src/main/java/com/tezza/lending/product/internal/repository/LoanProductRepository.java`
- Create: `src/main/java/com/tezza/lending/product/internal/repository/ProductFeeRepository.java`
- Create: `src/main/java/com/tezza/lending/product/internal/service/ProductServiceImpl.java`
- Create: `src/main/java/com/tezza/lending/product/api/ProductService.java`
- Create: `src/main/java/com/tezza/lending/product/api/dto/ProductRequest.java`
- Create: `src/main/java/com/tezza/lending/product/api/dto/ProductResponse.java`
- Create: `src/main/java/com/tezza/lending/product/api/dto/FeeRequest.java`
- Create: `src/main/java/com/tezza/lending/product/api/dto/FeeResponse.java`
- Create: `src/main/java/com/tezza/lending/product/web/ProductController.java`
- Test: `src/test/java/com/tezza/lending/product/ProductServiceTest.java`

- [ ] **Step 1: Create enums**

```java
// TenureType.java
package com.tezza.lending.product.internal.entity.enums;
public enum TenureType { DAYS, MONTHS }

// FeeType.java
package com.tezza.lending.product.internal.entity.enums;
public enum FeeType { SERVICE_FEE, DAILY_FEE, LATE_FEE }

// CalculationType.java
package com.tezza.lending.product.internal.entity.enums;
public enum CalculationType { FIXED, PERCENTAGE }
```

- [ ] **Step 2: Create LoanProduct entity**

```java
package com.tezza.lending.product.internal.entity;

import com.tezza.lending.product.internal.entity.enums.TenureType;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "LOAN_PRODUCTS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanProduct extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    private String description;

    @Column(name = "TENURE_VALUE", nullable = false)
    private int tenureValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "TENURE_TYPE", nullable = false, length = 10)
    private TenureType tenureType;

    @Column(name = "MIN_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "MAX_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "GRACE_PERIOD_DAYS", nullable = false)
    private int gracePeriodDays = 0;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ProductFee> fees = new ArrayList<>();
}
```

- [ ] **Step 3: Create ProductFee entity**

```java
package com.tezza.lending.product.internal.entity;

import com.tezza.lending.product.internal.entity.enums.CalculationType;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "PRODUCT_FEES")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductFee extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private LoanProduct product;

    @Enumerated(EnumType.STRING)
    @Column(name = "FEE_TYPE", nullable = false, length = 20)
    private FeeType feeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "CALCULATION_TYPE", nullable = false, length = 10)
    private CalculationType calculationType;

    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "DAYS_AFTER_DUE", nullable = false)
    private int daysAfterDue = 0;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;
}
```

- [ ] **Step 4: Create repositories**

```java
// LoanProductRepository.java
package com.tezza.lending.product.internal.repository;

import com.tezza.lending.product.internal.entity.LoanProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoanProductRepository extends JpaRepository<LoanProduct, UUID> {
    Page<LoanProduct> findByActive(boolean active, Pageable pageable);
    boolean existsByNameIgnoreCase(String name);
}
```

```java
// ProductFeeRepository.java
package com.tezza.lending.product.internal.repository;

import com.tezza.lending.product.internal.entity.ProductFee;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductFeeRepository extends JpaRepository<ProductFee, UUID> {
    List<ProductFee> findByProductIdAndActive(UUID productId, boolean active);
    List<ProductFee> findByProductIdAndFeeType(UUID productId, FeeType feeType);
}
```

- [ ] **Step 5: Create DTOs**

```java
// ProductRequest.java
package com.tezza.lending.product.api.dto;

import com.tezza.lending.product.internal.entity.enums.TenureType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank(message = "Product name required")
    @Size(max = 100)
    private String name;

    private String description;

    @Positive(message = "Tenure value must be positive")
    private int tenureValue;

    @NotNull(message = "Tenure type required")
    private TenureType tenureType;

    @NotNull @DecimalMin("0.01")
    private BigDecimal minAmount;

    @NotNull @DecimalMin("0.01")
    private BigDecimal maxAmount;

    @Min(0)
    private int gracePeriodDays = 0;
}
```

```java
// ProductResponse.java
package com.tezza.lending.product.api.dto;

import com.tezza.lending.product.internal.entity.LoanProduct;
import com.tezza.lending.product.internal.entity.enums.TenureType;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ProductResponse {
    private UUID id;
    private String name;
    private String description;
    private int tenureValue;
    private TenureType tenureType;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private int gracePeriodDays;
    private boolean active;
    private List<FeeResponse> fees;

    public static ProductResponse from(LoanProduct p) {
        ProductResponse r = new ProductResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setDescription(p.getDescription());
        r.setTenureValue(p.getTenureValue());
        r.setTenureType(p.getTenureType());
        r.setMinAmount(p.getMinAmount());
        r.setMaxAmount(p.getMaxAmount());
        r.setGracePeriodDays(p.getGracePeriodDays());
        r.setActive(p.isActive());
        r.setFees(p.getFees().stream().map(FeeResponse::from).toList());
        return r;
    }
}
```

```java
// FeeRequest.java
package com.tezza.lending.product.api.dto;

import com.tezza.lending.product.internal.entity.enums.CalculationType;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FeeRequest {
    @NotNull private FeeType feeType;
    @NotNull private CalculationType calculationType;
    @NotNull @DecimalMin("0.00") private BigDecimal amount;
    @Min(0) private int daysAfterDue = 0;
}
```

```java
// FeeResponse.java
package com.tezza.lending.product.api.dto;

import com.tezza.lending.product.internal.entity.ProductFee;
import com.tezza.lending.product.internal.entity.enums.CalculationType;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class FeeResponse {
    private UUID id;
    private FeeType feeType;
    private CalculationType calculationType;
    private BigDecimal amount;
    private int daysAfterDue;
    private boolean active;

    public static FeeResponse from(ProductFee f) {
        FeeResponse r = new FeeResponse();
        r.setId(f.getId());
        r.setFeeType(f.getFeeType());
        r.setCalculationType(f.getCalculationType());
        r.setAmount(f.getAmount());
        r.setDaysAfterDue(f.getDaysAfterDue());
        r.setActive(f.isActive());
        return r;
    }
}
```

- [ ] **Step 6: Create ProductService interface**

```java
package com.tezza.lending.product.api;

import com.tezza.lending.product.api.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse getProduct(UUID id);
    Page<ProductResponse> listProducts(boolean activeOnly, Pageable pageable);
    ProductResponse updateProduct(UUID id, ProductRequest request);
    void deactivateProduct(UUID id);
    ProductResponse addFee(UUID productId, FeeRequest request);
    void removeFee(UUID productId, UUID feeId);
}
```

- [ ] **Step 7: Create ProductServiceImpl**

```java
package com.tezza.lending.product.internal.service;

import com.tezza.lending.product.api.ProductService;
import com.tezza.lending.product.api.dto.*;
import com.tezza.lending.product.internal.entity.LoanProduct;
import com.tezza.lending.product.internal.entity.ProductFee;
import com.tezza.lending.product.internal.repository.LoanProductRepository;
import com.tezza.lending.product.internal.repository.ProductFeeRepository;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final LoanProductRepository productRepository;
    private final ProductFeeRepository feeRepository;

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Product with name '" + request.getName() + "' already exists");
        }
        if (request.getMaxAmount().compareTo(request.getMinAmount()) < 0) {
            throw new BusinessException("maxAmount must be >= minAmount");
        }
        LoanProduct product = LoanProduct.builder()
                .name(request.getName())
                .description(request.getDescription())
                .tenureValue(request.getTenureValue())
                .tenureType(request.getTenureType())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .gracePeriodDays(request.getGracePeriodDays())
                .active(true)
                .build();
        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("LoanProduct", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(boolean activeOnly, Pageable pageable) {
        if (activeOnly) {
            return productRepository.findByActive(true, pageable).map(ProductResponse::from);
        }
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    @Override
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        LoanProduct product = findProduct(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setTenureValue(request.getTenureValue());
        product.setTenureType(request.getTenureType());
        product.setMinAmount(request.getMinAmount());
        product.setMaxAmount(request.getMaxAmount());
        product.setGracePeriodDays(request.getGracePeriodDays());
        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    public void deactivateProduct(UUID id) {
        LoanProduct product = findProduct(id);
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    public ProductResponse addFee(UUID productId, FeeRequest request) {
        LoanProduct product = findProduct(productId);
        ProductFee fee = ProductFee.builder()
                .product(product)
                .feeType(request.getFeeType())
                .calculationType(request.getCalculationType())
                .amount(request.getAmount())
                .daysAfterDue(request.getDaysAfterDue())
                .active(true)
                .build();
        product.getFees().add(fee);
        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    public void removeFee(UUID productId, UUID feeId) {
        LoanProduct product = findProduct(productId);
        boolean removed = product.getFees().removeIf(f -> f.getId().equals(feeId));
        if (!removed) {
            throw new ResourceNotFoundException("ProductFee", feeId.toString());
        }
        productRepository.save(product);
    }

    private LoanProduct findProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanProduct", id.toString()));
    }
}
```

- [ ] **Step 8: Create ProductController**

```java
package com.tezza.lending.product.web;

import com.tezza.lending.product.api.ProductService;
import com.tezza.lending.product.api.dto.*;
import com.tezza.lending.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Loan Products")
@SecurityRequirement(name = "Bearer Authentication")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a loan product")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", productService.createProduct(request)));
    }

    @GetMapping
    @Operation(summary = "List loan products")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(productService.listProducts(activeOnly, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProduct(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a loan product")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated", productService.updateProduct(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a loan product")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        productService.deactivateProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated", null));
    }

    @PostMapping("/{id}/fees")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a fee to a product")
    public ResponseEntity<ApiResponse<ProductResponse>> addFee(
            @PathVariable UUID id, @Valid @RequestBody FeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fee added", productService.addFee(id, request)));
    }

    @DeleteMapping("/{id}/fees/{feeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a fee from a product")
    public ResponseEntity<ApiResponse<Void>> removeFee(@PathVariable UUID id, @PathVariable UUID feeId) {
        productService.removeFee(id, feeId);
        return ResponseEntity.ok(ApiResponse.success("Fee removed", null));
    }
}
```

- [ ] **Step 9: Write failing tests first**

Create `src/test/java/com/tezza/lending/product/ProductServiceTest.java`:

```java
package com.tezza.lending.product;

import com.tezza.lending.product.api.dto.FeeRequest;
import com.tezza.lending.product.api.dto.ProductRequest;
import com.tezza.lending.product.api.dto.ProductResponse;
import com.tezza.lending.product.internal.entity.LoanProduct;
import com.tezza.lending.product.internal.entity.enums.CalculationType;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import com.tezza.lending.product.internal.entity.enums.TenureType;
import com.tezza.lending.product.internal.repository.LoanProductRepository;
import com.tezza.lending.product.internal.repository.ProductFeeRepository;
import com.tezza.lending.product.internal.service.ProductServiceImpl;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock LoanProductRepository productRepository;
    @Mock ProductFeeRepository feeRepository;
    @InjectMocks ProductServiceImpl productService;

    private ProductRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ProductRequest();
        validRequest.setName("Test Loan");
        validRequest.setTenureValue(30);
        validRequest.setTenureType(TenureType.DAYS);
        validRequest.setMinAmount(BigDecimal.valueOf(1000));
        validRequest.setMaxAmount(BigDecimal.valueOf(50000));
    }

    @Test
    void createProduct_setsActiveTrue() {
        when(productRepository.existsByNameIgnoreCase("Test Loan")).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> {
            LoanProduct p = inv.getArgument(0);
            p.setFees(new ArrayList<>());
            return p;
        });

        ProductResponse result = productService.createProduct(validRequest);

        assertThat(result.isActive()).isTrue();
        verify(productRepository).save(any(LoanProduct.class));
    }

    @Test
    void createProduct_duplicateName_throwsBusinessException() {
        when(productRepository.existsByNameIgnoreCase("Test Loan")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createProduct_maxAmountLessThanMin_throwsBusinessException() {
        validRequest.setMaxAmount(BigDecimal.valueOf(500)); // less than min 1000

        when(productRepository.existsByNameIgnoreCase(any())).thenReturn(false);

        assertThatThrownBy(() -> productService.createProduct(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("maxAmount");
    }

    @Test
    void deactivateProduct_setsActiveFalse() {
        LoanProduct product = LoanProduct.builder().id(UUID.randomUUID()).active(true).fees(new ArrayList<>()).build();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        productService.deactivateProduct(product.getId());

        assertThat(product.isActive()).isFalse();
    }

    @Test
    void getProduct_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addFee_linksToProduct() {
        LoanProduct product = LoanProduct.builder().id(UUID.randomUUID()).active(true).fees(new ArrayList<>()).build();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        FeeRequest feeRequest = new FeeRequest();
        feeRequest.setFeeType(FeeType.SERVICE_FEE);
        feeRequest.setCalculationType(CalculationType.PERCENTAGE);
        feeRequest.setAmount(BigDecimal.valueOf(5));
        feeRequest.setDaysAfterDue(0);

        ProductResponse result = productService.addFee(product.getId(), feeRequest);

        assertThat(product.getFees()).hasSize(1);
        assertThat(product.getFees().get(0).getFeeType()).isEqualTo(FeeType.SERVICE_FEE);
    }

    @Test
    void listProducts_returnsPaginated() {
        LoanProduct product = LoanProduct.builder().id(UUID.randomUUID()).active(true).fees(new ArrayList<>()).build();
        when(productRepository.findByActive(true, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(product)));

        var page = productService.listProducts(true, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
    }
}
```

- [ ] **Step 10: Run tests to verify pass**

```bash
mvn test -Dtest="ProductServiceTest" -q
```

Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/tezza/lending/product/ \
        src/test/java/com/tezza/lending/product/
git commit -m "feat: add product module — loan products, fee configuration, CRUD APIs"
```

---

## Task 6: Customer Module

**Files:**
- Create: `src/main/java/com/tezza/lending/customer/internal/entity/Customer.java`
- Create: `src/main/java/com/tezza/lending/customer/internal/entity/CustomerLoanLimit.java`
- Create: `src/main/java/com/tezza/lending/customer/internal/entity/enums/CustomerStatus.java`
- Create: `src/main/java/com/tezza/lending/customer/internal/repository/CustomerRepository.java`
- Create: `src/main/java/com/tezza/lending/customer/internal/repository/CustomerLoanLimitRepository.java`
- Create: `src/main/java/com/tezza/lending/customer/internal/service/CustomerServiceImpl.java`
- Create: `src/main/java/com/tezza/lending/customer/api/CustomerService.java`
- Create: `src/main/java/com/tezza/lending/customer/api/dto/CustomerRequest.java`
- Create: `src/main/java/com/tezza/lending/customer/api/dto/CustomerResponse.java`
- Create: `src/main/java/com/tezza/lending/customer/api/dto/LoanLimitRequest.java`
- Create: `src/main/java/com/tezza/lending/customer/api/dto/LoanLimitResponse.java`
- Create: `src/main/java/com/tezza/lending/customer/web/CustomerController.java`
- Test: `src/test/java/com/tezza/lending/customer/CustomerServiceTest.java`

- [ ] **Step 1: Create CustomerStatus enum**

```java
package com.tezza.lending.customer.internal.entity.enums;
public enum CustomerStatus { ACTIVE, SUSPENDED, BLACKLISTED }
```

- [ ] **Step 2: Create Customer entity**

```java
package com.tezza.lending.customer.internal.entity;

import com.tezza.lending.customer.internal.entity.enums.CustomerStatus;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "CUSTOMERS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "FIRST_NAME", nullable = false, length = 100)
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false, length = 100)
    private String lastName;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "PHONE", length = 20)
    private String phone;

    @Column(name = "NATIONAL_ID", unique = true, length = 50)
    private String nationalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;
}
```

- [ ] **Step 3: Create CustomerLoanLimit entity**

```java
package com.tezza.lending.customer.internal.entity;

import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CUSTOMER_LOAN_LIMITS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerLoanLimit extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "MIN_LIMIT", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal minLimit = BigDecimal.ZERO;

    @Column(name = "MAX_LIMIT", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxLimit;

    @Column(name = "CURRENT_LIMIT", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentLimit;

    @Column(name = "CREDIT_SCORE", nullable = false)
    @Builder.Default
    private int creditScore = 0;

    @Column(name = "LAST_REVIEWED_AT")
    private LocalDateTime lastReviewedAt;
}
```

- [ ] **Step 4: Create repositories**

```java
// CustomerRepository.java
package com.tezza.lending.customer.internal.repository;

import com.tezza.lending.customer.internal.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByNationalId(String nationalId);
    boolean existsByEmail(String email);
    boolean existsByNationalId(String nationalId);
}
```

```java
// CustomerLoanLimitRepository.java
package com.tezza.lending.customer.internal.repository;

import com.tezza.lending.customer.internal.entity.CustomerLoanLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CustomerLoanLimitRepository extends JpaRepository<CustomerLoanLimit, UUID> {
    Optional<CustomerLoanLimit> findByCustomerId(UUID customerId);
}
```

- [ ] **Step 5: Create DTOs**

```java
// CustomerRequest.java
package com.tezza.lending.customer.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CustomerRequest {
    @NotBlank @Size(max = 100) private String firstName;
    @NotBlank @Size(max = 100) private String lastName;
    @NotBlank @Email private String email;
    @Size(max = 20) private String phone;
    @Size(max = 50) private String nationalId;
}
```

```java
// CustomerResponse.java
package com.tezza.lending.customer.api.dto;

import com.tezza.lending.customer.internal.entity.Customer;
import com.tezza.lending.customer.internal.entity.enums.CustomerStatus;
import lombok.Data;
import java.util.UUID;

@Data
public class CustomerResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String nationalId;
    private CustomerStatus status;

    public static CustomerResponse from(Customer c) {
        CustomerResponse r = new CustomerResponse();
        r.setId(c.getId());
        r.setFirstName(c.getFirstName());
        r.setLastName(c.getLastName());
        r.setEmail(c.getEmail());
        r.setPhone(c.getPhone());
        r.setNationalId(c.getNationalId());
        r.setStatus(c.getStatus());
        return r;
    }
}
```

```java
// LoanLimitRequest.java
package com.tezza.lending.customer.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanLimitRequest {
    @NotNull @DecimalMin("0.00") private BigDecimal minLimit;
    @NotNull @DecimalMin("0.01") private BigDecimal maxLimit;
    @NotNull @DecimalMin("0.00") private BigDecimal currentLimit;
    @Min(0) @Max(1000) private int creditScore;
}
```

```java
// LoanLimitResponse.java
package com.tezza.lending.customer.api.dto;

import com.tezza.lending.customer.internal.entity.CustomerLoanLimit;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LoanLimitResponse {
    private UUID id;
    private UUID customerId;
    private BigDecimal minLimit;
    private BigDecimal maxLimit;
    private BigDecimal currentLimit;
    private int creditScore;
    private LocalDateTime lastReviewedAt;

    public static LoanLimitResponse from(CustomerLoanLimit l) {
        LoanLimitResponse r = new LoanLimitResponse();
        r.setId(l.getId());
        r.setCustomerId(l.getCustomer().getId());
        r.setMinLimit(l.getMinLimit());
        r.setMaxLimit(l.getMaxLimit());
        r.setCurrentLimit(l.getCurrentLimit());
        r.setCreditScore(l.getCreditScore());
        r.setLastReviewedAt(l.getLastReviewedAt());
        return r;
    }
}
```

- [ ] **Step 6: Create CustomerService interface**

```java
package com.tezza.lending.customer.api;

import com.tezza.lending.customer.api.dto.*;
import java.util.UUID;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRequest request);
    CustomerResponse getCustomer(UUID id);
    CustomerResponse updateCustomer(UUID id, CustomerRequest request);
    LoanLimitResponse getLoanLimit(UUID customerId);
    LoanLimitResponse updateLoanLimit(UUID customerId, LoanLimitRequest request);
}
```

- [ ] **Step 7: Create CustomerServiceImpl**

```java
package com.tezza.lending.customer.internal.service;

import com.tezza.lending.customer.api.CustomerService;
import com.tezza.lending.customer.api.dto.*;
import com.tezza.lending.customer.internal.entity.Customer;
import com.tezza.lending.customer.internal.entity.CustomerLoanLimit;
import com.tezza.lending.customer.internal.entity.enums.CustomerStatus;
import com.tezza.lending.customer.internal.repository.CustomerLoanLimitRepository;
import com.tezza.lending.customer.internal.repository.CustomerRepository;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerLoanLimitRepository loanLimitRepository;

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }
        if (request.getNationalId() != null && customerRepository.existsByNationalId(request.getNationalId())) {
            throw new BusinessException("National ID already registered: " + request.getNationalId());
        }
        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .nationalId(request.getNationalId())
                .status(CustomerStatus.ACTIVE)
                .build();
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID id) {
        return customerRepository.findById(id)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id.toString()));
    }

    @Override
    public CustomerResponse updateCustomer(UUID id, CustomerRequest request) {
        Customer customer = findCustomer(id);
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhone(request.getPhone());
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public LoanLimitResponse getLoanLimit(UUID customerId) {
        findCustomer(customerId);
        return loanLimitRepository.findByCustomerId(customerId)
                .map(LoanLimitResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("LoanLimit for customer", customerId.toString()));
    }

    @Override
    public LoanLimitResponse updateLoanLimit(UUID customerId, LoanLimitRequest request) {
        Customer customer = findCustomer(customerId);
        if (customer.getStatus() == CustomerStatus.BLACKLISTED) {
            throw new BusinessException("Cannot update loan limit for blacklisted customer");
        }
        CustomerLoanLimit limit = loanLimitRepository.findByCustomerId(customerId)
                .orElse(CustomerLoanLimit.builder().customer(customer).build());
        limit.setMinLimit(request.getMinLimit());
        limit.setMaxLimit(request.getMaxLimit());
        limit.setCurrentLimit(request.getCurrentLimit());
        limit.setCreditScore(request.getCreditScore());
        limit.setLastReviewedAt(LocalDateTime.now());
        return LoanLimitResponse.from(loanLimitRepository.save(limit));
    }

    private Customer findCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id.toString()));
    }
}
```

- [ ] **Step 8: Create CustomerController**

```java
package com.tezza.lending.customer.web;

import com.tezza.lending.customer.api.CustomerService;
import com.tezza.lending.customer.api.dto.*;
import com.tezza.lending.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created", customerService.createCustomer(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getCustomer(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update customer details")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer updated", customerService.updateCustomer(id, request)));
    }

    @GetMapping("/{id}/loan-limit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get customer loan limit")
    public ResponseEntity<ApiResponse<LoanLimitResponse>> getLoanLimit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getLoanLimit(id)));
    }

    @PutMapping("/{id}/loan-limit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update customer loan limit")
    public ResponseEntity<ApiResponse<LoanLimitResponse>> updateLoanLimit(
            @PathVariable UUID id, @Valid @RequestBody LoanLimitRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Loan limit updated", customerService.updateLoanLimit(id, request)));
    }
}
```

- [ ] **Step 9: Write failing tests first**

Create `src/test/java/com/tezza/lending/customer/CustomerServiceTest.java`:

```java
package com.tezza.lending.customer;

import com.tezza.lending.customer.api.dto.CustomerRequest;
import com.tezza.lending.customer.api.dto.LoanLimitRequest;
import com.tezza.lending.customer.internal.entity.Customer;
import com.tezza.lending.customer.internal.entity.CustomerLoanLimit;
import com.tezza.lending.customer.internal.entity.enums.CustomerStatus;
import com.tezza.lending.customer.internal.repository.CustomerLoanLimitRepository;
import com.tezza.lending.customer.internal.repository.CustomerRepository;
import com.tezza.lending.customer.internal.service.CustomerServiceImpl;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock CustomerLoanLimitRepository loanLimitRepository;
    @InjectMocks CustomerServiceImpl customerService;

    private CustomerRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CustomerRequest();
        validRequest.setFirstName("Alice");
        validRequest.setLastName("Wanjiku");
        validRequest.setEmail("alice@example.com");
        validRequest.setNationalId("12345678");
    }

    @Test
    void createCustomer_success() {
        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(customerRepository.existsByNationalId("12345678")).thenReturn(false);
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = customerService.createCustomer(validRequest);

        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void createCustomer_duplicateEmail_throwsBusinessException() {
        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void getCustomer_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateLoanLimit_blacklistedCustomer_throwsBusinessException() {
        UUID id = UUID.randomUUID();
        Customer blacklisted = Customer.builder().id(id).status(CustomerStatus.BLACKLISTED).build();
        when(customerRepository.findById(id)).thenReturn(Optional.of(blacklisted));

        LoanLimitRequest req = new LoanLimitRequest();
        req.setMinLimit(BigDecimal.ZERO);
        req.setMaxLimit(BigDecimal.valueOf(50000));
        req.setCurrentLimit(BigDecimal.valueOf(50000));

        assertThatThrownBy(() -> customerService.updateLoanLimit(id, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("blacklisted");
    }

    @Test
    void updateLoanLimit_createsNewLimitIfAbsent() {
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder().id(id).status(CustomerStatus.ACTIVE).build();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(loanLimitRepository.findByCustomerId(id)).thenReturn(Optional.empty());
        when(loanLimitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanLimitRequest req = new LoanLimitRequest();
        req.setMinLimit(BigDecimal.valueOf(1000));
        req.setMaxLimit(BigDecimal.valueOf(50000));
        req.setCurrentLimit(BigDecimal.valueOf(50000));
        req.setCreditScore(700);

        var result = customerService.updateLoanLimit(id, req);

        assertThat(result.getMaxLimit()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(result.getCreditScore()).isEqualTo(700);
    }
}
```

- [ ] **Step 10: Run tests**

```bash
mvn test -Dtest="CustomerServiceTest" -q
```

Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/tezza/lending/customer/ \
        src/test/java/com/tezza/lending/customer/
git commit -m "feat: add customer module — profiles, loan limits, CRUD APIs"
```
