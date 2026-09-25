package com.hospitality.booking.client;

import com.hospitality.booking.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", url = "${services.customer-service.url}")
public interface CustomerClient {

    @GetMapping("/api/v1/customers/{id}/validate")
    ApiResponse<Boolean> validateCustomerActive(@PathVariable("id") Long id);
}
