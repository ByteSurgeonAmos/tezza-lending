package com.tezza.lending.product.web;

import com.tezza.lending.product.api.ProductService;
import com.tezza.lending.product.api.dto.FeeRequest;
import com.tezza.lending.product.api.dto.ProductRequest;
import com.tezza.lending.product.api.dto.ProductResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Loan Products")
@SecurityRequirement(name = "Bearer Authentication")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
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
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update a loan product")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated", productService.updateProduct(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Deactivate a loan product")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        productService.deactivateProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated", null));
    }

    @PostMapping("/{id}/fees")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Add a fee to a product")
    public ResponseEntity<ApiResponse<ProductResponse>> addFee(
            @PathVariable UUID id, @Valid @RequestBody FeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fee added", productService.addFee(id, request)));
    }

    @DeleteMapping("/{id}/fees/{feeId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Remove a fee from a product")
    public ResponseEntity<ApiResponse<Void>> removeFee(@PathVariable UUID id, @PathVariable UUID feeId) {
        productService.removeFee(id, feeId);
        return ResponseEntity.ok(ApiResponse.success("Fee removed", null));
    }
}
