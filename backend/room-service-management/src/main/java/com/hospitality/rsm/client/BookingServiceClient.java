package com.hospitality.rsm.client;

import com.hospitality.rsm.dto.ApiResponse;
import com.hospitality.rsm.dto.BookingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "booking-service", url = "${services.booking-service.url}")
public interface BookingServiceClient {

    @GetMapping("/api/v1/bookings/{id}")
    ApiResponse<BookingDto> getBookingById(@PathVariable("id") Long id);
}
