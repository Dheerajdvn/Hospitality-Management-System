package com.hospitality.inventory.client;

import com.hospitality.inventory.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "hotel-service", url = "${services.hotel-service.url}")
public interface HotelClient {

    @GetMapping("/api/v1/hotels/{id}/validate")
    ApiResponse<Boolean> validateHotelActive(@PathVariable("id") Long id);
}
