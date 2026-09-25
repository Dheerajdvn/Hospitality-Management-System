package com.hospitality.billing.client;

import com.hospitality.billing.dto.ApiResponse;
import com.hospitality.billing.dto.BookingDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "booking-service", url = "${services.booking-service.url}")
public interface BookingClient {

    @GetMapping("/api/v1/bookings/{id}")
    ApiResponse<BookingDetailResponse> getBookingById(@PathVariable("id") Long id);

    @PostMapping("/api/v1/bookings/{id}/confirm-payment")
    ApiResponse<Object> confirmBookingPayment(@PathVariable("id") Long id);
}
