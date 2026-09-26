package com.hospitality.customer.service;

import com.hospitality.customer.dto.CustomerCreateRequest;
import com.hospitality.customer.dto.CustomerResponse;
import com.hospitality.customer.dto.CustomerUpdateRequest;

import java.util.List;

public interface CustomerService {

    CustomerResponse createCustomer(CustomerCreateRequest request);

    CustomerResponse getCustomerById(Long id);

    CustomerResponse getCustomerByUserId(Long userId);

    CustomerResponse getOrCreateCustomerByUserId(Long userId, String email, String username);

    CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request);

    List<CustomerResponse> getAllCustomers();

    boolean validateCustomerActive(Long id);
}
