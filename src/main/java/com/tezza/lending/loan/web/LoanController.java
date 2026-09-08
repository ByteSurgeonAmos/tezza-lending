package com.tezza.lending.loan.web;

import com.tezza.lending.loan.api.LoanService;
import com.tezza.lending.loan.api.dto.*;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loans")
@SecurityRequirement(name = "Bearer Authentication")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Apply and disburse a loan")
    public ResponseEntity<ApiResponse<LoanResponse>> disburse(@Valid @RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Loan disbursed", loanService.disburseLoan(request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List loans with optional status filter")
    public ResponseEntity<ApiResponse<Page<LoanResponse>>> list(
            @RequestParam(required = false) LoanStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(loanService.listLoans(status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Get loan by ID")
    public ResponseEntity<ApiResponse<LoanResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getLoan(id)));
    }

    @GetMapping("/{id}/installments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Get loan installment schedule")
    public ResponseEntity<ApiResponse<List<InstallmentResponse>>> getInstallments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getInstallments(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel an open loan")
    public ResponseEntity<ApiResponse<LoanResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Loan cancelled", loanService.cancelLoan(id)));
    }

    @PostMapping("/{id}/write-off")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Write off an overdue loan")
    public ResponseEntity<ApiResponse<LoanResponse>> writeOff(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Loan written off", loanService.writeOffLoan(id)));
    }
}
