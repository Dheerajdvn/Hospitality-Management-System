package com.hospitality.booking.controller;

import com.hospitality.booking.dto.ApiResponse;
import com.hospitality.booking.dto.BookingRequest;
import com.hospitality.booking.dto.BookingResponse;
import com.hospitality.booking.dto.BookingStatusUpdateRequest;
import com.hospitality.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return new ResponseEntity<>(
                ApiResponse.success("Booking hold placed successfully. Please complete payment within 15 minutes.", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable("id") Long id) {
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success("Booking details retrieved", response));
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(@PathVariable("reference") String reference) {
        BookingResponse response = bookingService.getBookingByReference(reference);
        return ResponseEntity.ok(ApiResponse.success("Booking details retrieved", response));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByCustomerId(@PathVariable("customerId") Long customerId) {
        List<BookingResponse> bookings = bookingService.getBookingsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer booking history retrieved", bookings));
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByHotelId(@PathVariable("hotelId") Long hotelId) {
        List<BookingResponse> bookings = bookingService.getBookingsByHotelId(hotelId);
        return ResponseEntity.ok(ApiResponse.success("Hotel bookings retrieved", bookings));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBookingStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody BookingStatusUpdateRequest request) {
        BookingResponse response = bookingService.updateBookingStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Booking status updated successfully", response));
    }

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmPayment(@PathVariable("id") Long id) {
        BookingResponse response = bookingService.confirmBookingPayment(id);
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed and reservation finalized", response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", defaultValue = "Cancelled by user") String reason) {
        BookingResponse response = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled and room hold released", response));
    }
}
