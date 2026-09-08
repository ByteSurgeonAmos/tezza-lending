package com.tezza.lending.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Base64;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    @Value("${tezza.auth.admin-client-secret}")
    private String adminClientSecret;

    @Value("${tezza.auth.mobile-client-secret}")
    private String mobileClientSecret;

    @Value("${tezza.auth.jwt-private-key:}")
    private String jwtPrivateKeyPem;

    @Value("${tezza.issuer-uri:${TEZZA_ISSUER_URI:http://localhost:8080}}")
    private String issuerUri;

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
    public RegisteredClientRepository registeredClientRepository(DataSource dataSource, PasswordEncoder encoder) {
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(
                new org.springframework.jdbc.core.JdbcTemplate(dataSource));

        if (repository.findByClientId("tezza-admin-client") == null) {
            RegisteredClient adminClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("tezza-admin-client")
                    .clientSecret(encoder.encode(adminClientSecret))
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

        if (repository.findByClientId("tezza-mobile-client") == null) {
            RegisteredClient mobileClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("tezza-mobile-client")
                    .clientSecret(encoder.encode(mobileClientSecret))
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

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            if (context.getTokenType().getValue().equals("access_token")) {
                JwtClaimsSet.Builder claims = context.getClaims();
                var scopes = context.getAuthorizedScopes();
                // MAP SCOPES TO SPRING SECURITY ROLE_ AUTHORITIES
                var roles = scopes.stream()
                        .map(scope -> "ROLE_" + scope.toUpperCase())
                        .toList();
                claims.claim("roles", roles);
                claims.claim("client_id", context.getRegisteredClient().getClientId());
            }
        };
    }

    /**
     * RSA signing key for JWT tokens.
     * In production: set TEZZA_JWT_PRIVATE_KEY to a stable base64-encoded PKCS8 private key.
     * Without it, a new key is generated each restart — valid tokens are invalidated on restart.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        KeyPair keyPair = loadOrGenerateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private KeyPair loadOrGenerateKeyPair() throws Exception {
        if (jwtPrivateKeyPem != null && !jwtPrivateKeyPem.isBlank()) {
            // LOAD FROM CONFIGURED PRIVATE KEY (base64-encoded PKCS8 DER)
            byte[] decoded = Base64.getDecoder().decode(jwtPrivateKeyPem.replaceAll("\\s+", ""));
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(decoded);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(spec);
            // DERIVE PUBLIC KEY FROM PRIVATE KEY
            java.security.interfaces.RSAPrivateCrtKey crtKey = (java.security.interfaces.RSAPrivateCrtKey) privateKey;
            java.security.spec.RSAPublicKeySpec pubSpec = new java.security.spec.RSAPublicKeySpec(
                    crtKey.getModulus(), crtKey.getPublicExponent());
            RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(pubSpec);
            return new KeyPair(publicKey, privateKey);
        }
        log.warn("TEZZA_JWT_PRIVATE_KEY not set — generating ephemeral RSA key. " +
                 "Tokens will be invalidated on restart. Set this env var in production.");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuerUri)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
