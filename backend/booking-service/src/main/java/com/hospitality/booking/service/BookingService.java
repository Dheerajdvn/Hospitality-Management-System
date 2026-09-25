package com.hospitality.booking.service;

import com.hospitality.booking.dto.BookingRequest;
import com.hospitality.booking.dto.BookingResponse;
import com.hospitality.booking.dto.BookingStatusUpdateRequest;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(BookingRequest request);

    BookingResponse getBookingById(Long id);

    BookingResponse getBookingByReference(String reference);

    List<BookingResponse> getBookingsByCustomerId(Long customerId);

    List<BookingResponse> getBookingsByHotelId(Long hotelId);

    BookingResponse updateBookingStatus(Long id, BookingStatusUpdateRequest request);

    BookingResponse confirmBookingPayment(Long id);

    BookingResponse cancelBooking(Long id, String reason);

    int expireUnpaidHolds();
}
