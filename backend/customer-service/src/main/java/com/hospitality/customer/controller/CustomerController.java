package com.hospitality.customer.controller;

import com.hospitality.customer.dto.*;
import com.hospitality.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return new ResponseEntity<>(
                ApiResponse.success("Customer profile created successfully", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable("id") Long id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success("Customer profile retrieved", response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByUserId(@PathVariable("userId") Long userId) {
        CustomerResponse response = customerService.getCustomerByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Customer profile retrieved by user ID", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCurrentCustomer(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long queryUserId) {
        Long resolvedUserId = headerUserId != null ? headerUserId : queryUserId;
        if (resolvedUserId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User identity not provided in header or parameter"));
        }
        CustomerResponse response = customerService.getCustomerByUserId(resolvedUserId);
        return ResponseEntity.ok(ApiResponse.success("Current customer profile", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable("id") Long id,
            @RequestBody CustomerUpdateRequest request) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Customer profile updated successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        List<CustomerResponse> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success("All customer profiles retrieved", customers));
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateCustomer(@PathVariable("id") Long id) {
        boolean isValid = customerService.validateCustomerActive(id);
        return ResponseEntity.ok(ApiResponse.success(
                isValid ? "Customer is active" : "Customer not found or inactive",
                isValid
        ));
    }
}
