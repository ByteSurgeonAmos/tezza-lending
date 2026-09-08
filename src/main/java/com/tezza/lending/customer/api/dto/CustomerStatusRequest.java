package com.tezza.lending.customer.api.dto;

import com.tezza.lending.customer.internal.entity.enums.CustomerStatus;
import jakarta.validation.constraints.NotNull;

public class CustomerStatusRequest {

    @NotNull(message = "Status is required")
    private CustomerStatus status;

    public CustomerStatus getStatus() { return status; }
    public void setStatus(CustomerStatus status) { this.status = status; }
}
