package com.tezza.lending.customer.api.dto;

import com.tezza.lending.customer.internal.entity.Customer;
import com.tezza.lending.customer.internal.entity.enums.CustomerStatus;

import java.util.UUID;

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
        r.id = c.getId();
        r.firstName = c.getFirstName();
        r.lastName = c.getLastName();
        r.email = c.getEmail();
        r.phone = c.getPhone();
        r.nationalId = c.getNationalId();
        r.status = c.getStatus();
        return r;
    }

    public UUID getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getNationalId() { return nationalId; }
    public CustomerStatus getStatus() { return status; }
}
