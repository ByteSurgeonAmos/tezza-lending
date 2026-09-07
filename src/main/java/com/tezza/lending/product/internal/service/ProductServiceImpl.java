package com.tezza.lending.product.internal.service;

import com.tezza.lending.product.api.ProductService;
import com.tezza.lending.product.api.dto.FeeRequest;
import com.tezza.lending.product.api.dto.ProductRequest;
import com.tezza.lending.product.api.dto.ProductResponse;
import com.tezza.lending.product.internal.entity.LoanProduct;
import com.tezza.lending.product.internal.entity.ProductFee;
import com.tezza.lending.product.internal.repository.LoanProductRepository;
import com.tezza.lending.product.internal.repository.ProductFeeRepository;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final LoanProductRepository productRepository;
    private final ProductFeeRepository feeRepository;

    public ProductServiceImpl(LoanProductRepository productRepository, ProductFeeRepository feeRepository) {
        this.productRepository = productRepository;
        this.feeRepository = feeRepository;
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Product with name '" + request.getName() + "' already exists");
        }
        if (request.getMaxAmount().compareTo(request.getMinAmount()) < 0) {
            throw new BusinessException("maxAmount must be >= minAmount");
        }
        LoanProduct product = new LoanProduct();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setTenureValue(request.getTenureValue());
        product.setTenureType(request.getTenureType());
        product.setMinAmount(request.getMinAmount());
        product.setMaxAmount(request.getMaxAmount());
        product.setGracePeriodDays(request.getGracePeriodDays());
        product.setActive(true);
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
        ProductFee fee = new ProductFee();
        fee.setProduct(product);
        fee.setFeeType(request.getFeeType());
        fee.setCalculationType(request.getCalculationType());
        fee.setAmount(request.getAmount());
        fee.setDaysAfterDue(request.getDaysAfterDue());
        fee.setActive(true);
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
