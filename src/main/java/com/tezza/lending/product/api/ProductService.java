package com.tezza.lending.product.api;

import com.tezza.lending.product.api.dto.FeeRequest;
import com.tezza.lending.product.api.dto.FeeResponse;
import com.tezza.lending.product.api.dto.ProductRequest;
import com.tezza.lending.product.api.dto.ProductResponse;
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
