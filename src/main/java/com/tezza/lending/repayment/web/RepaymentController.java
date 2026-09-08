package com.tezza.lending.repayment.web;

import com.tezza.lending.repayment.api.RepaymentService;
import com.tezza.lending.repayment.api.dto.RepaymentRequest;
import com.tezza.lending.repayment.api.dto.RepaymentResponse;
import com.tezza.lending.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Repayments")
@SecurityRequirement(name = "Bearer Authentication")
public class RepaymentController {

    private final RepaymentService repaymentService;

    public RepaymentController(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @PostMapping("/api/v1/repayments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Process a loan repayment")
    public ResponseEntity<ApiResponse<RepaymentResponse>> process(@Valid @RequestBody RepaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Repayment processed", repaymentService.processRepayment(request)));
    }

    @GetMapping("/api/v1/loans/{loanId}/repayments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Get repayment history for a loan")
    public ResponseEntity<ApiResponse<List<RepaymentResponse>>> getHistory(@PathVariable UUID loanId) {
        return ResponseEntity.ok(ApiResponse.success(repaymentService.getLoanRepayments(loanId)));
    }
}
