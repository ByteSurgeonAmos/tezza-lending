package com.tezza.lending.auth.web;

import com.tezza.lending.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@Tag(name = "OAuth2 Client Registration")
public class AuthController {

    private final RegisteredClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(RegisteredClientRepository clientRepository, PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record ClientRegistrationRequest(
            @NotBlank @Size(max = 100) String clientId,
            @NotBlank @Size(min = 16) String clientSecret,
            @NotBlank @Size(max = 200) String clientName,
            @Pattern(regexp = "ADMIN|CUSTOMER", message = "scope must be ADMIN or CUSTOMER") String scope
    ) {
        public ClientRegistrationRequest {
            if (scope == null) scope = "CUSTOMER";
        }
    }

    public record ClientRegistrationResponse(
            String clientId,
            String clientName,
            String scope,
            String tokenEndpoint
    ) {
        public ClientRegistrationResponse(String clientId, String clientName, String scope) {
            this(clientId, clientName, scope,
                    "POST /oauth2/token (Basic auth: clientId:clientSecret, grant_type=client_credentials)");
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new OAuth2 client (microservice/app)")
    public ResponseEntity<ApiResponse<ClientRegistrationResponse>> register(
            @Valid @RequestBody ClientRegistrationRequest request) {

        if (clientRepository.findByClientId(request.clientId()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Client registration failed"));
        }

        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(request.clientId())
                .clientSecret(passwordEncoder.encode(request.clientSecret()))
                .clientName(request.clientName())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(request.scope())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .build())
                .build();

        clientRepository.save(client);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Client registered",
                        new ClientRegistrationResponse(request.clientId(), request.clientName(), request.scope())));
    }
}
