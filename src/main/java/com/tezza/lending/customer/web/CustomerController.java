package com.tezza.lending.customer.web;

import com.tezza.lending.customer.api.CustomerService;
import com.tezza.lending.customer.api.dto.CustomerRequest;
import com.tezza.lending.customer.api.dto.CustomerResponse;
import com.tezza.lending.customer.api.dto.LoanLimitRequest;
import com.tezza.lending.customer.api.dto.LoanLimitResponse;
import com.tezza.lending.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create a customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created", customerService.createCustomer(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getCustomer(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update customer details")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer updated", customerService.updateCustomer(id, request)));
    }

    @GetMapping("/{id}/loan-limit")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get customer loan limit")
    public ResponseEntity<ApiResponse<LoanLimitResponse>> getLoanLimit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getLoanLimit(id)));
    }

    @PutMapping("/{id}/loan-limit")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update customer loan limit")
    public ResponseEntity<ApiResponse<LoanLimitResponse>> updateLoanLimit(
            @PathVariable UUID id, @Valid @RequestBody LoanLimitRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Loan limit updated", customerService.updateLoanLimit(id, request)));
    }
}
