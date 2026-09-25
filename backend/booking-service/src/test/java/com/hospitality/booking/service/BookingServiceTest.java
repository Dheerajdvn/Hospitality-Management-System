package com.hospitality.booking.service;

import com.hospitality.booking.client.CustomerServiceClientDelegate;
import com.hospitality.booking.client.RoomServiceClientDelegate;
import com.hospitality.booking.dto.*;
import com.hospitality.booking.entity.Booking;
import com.hospitality.booking.entity.BookingStatus;
import com.hospitality.booking.entity.PaymentStatus;
import com.hospitality.booking.event.BookingCancelledEvent;
import com.hospitality.booking.event.BookingConfirmedEvent;
import com.hospitality.booking.event.BookingCreatedEvent;
import com.hospitality.booking.exception.BadRequestException;
import com.hospitality.booking.exception.BookingConflictException;
import com.hospitality.booking.publisher.BookingEventPublisher;
import com.hospitality.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CustomerServiceClientDelegate customerClientDelegate;

    @Mock
    private RoomServiceClientDelegate roomClientDelegate;

    @Mock
    private BookingEventPublisher eventPublisher;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private RoomDetailResponse sampleRoom;
    private BookingRequest sampleRequest;
    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "holdDurationMinutes", 15);

        sampleRoom = RoomDetailResponse.builder()
                .id(1L)
                .hotelId(1L)
                .roomNumber("101")
                .type("DELUXE")
                .status("AVAILABLE")
                .basePrice(new BigDecimal("5000.00"))
                .capacity(2)
                .floorNumber(1)
                .amenities(Set.of("WiFi", "AC"))
                .build();

        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(5); // 3 nights

        sampleRequest = BookingRequest.builder()
                .customerId(1L)
                .roomId(1L)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .numberOfGuests(2)
                .specialRequests("Sea view")
                .build();

        sampleBooking = Booking.builder()
                .id(100L)
                .bookingReference("HMS-BK-TEST1234")
                .customerId(1L)
                .roomId(1L)
                .hotelId(1L)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .numberOfNights(3)
                .numberOfGuests(2)
                .roomPricePerNight(new BigDecimal("5000.00"))
                .totalAmount(new BigDecimal("15000.00"))
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.UNPAID)
                .holdExpiresAt(LocalDateTime.now().plusMinutes(15))
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Should successfully place booking hold, update room status via resilient delegate, and publish Kafka event")
    void testCreateBooking_Success() {
        when(customerClientDelegate.validateCustomerActive(1L)).thenReturn(true);
        when(roomClientDelegate.getRoomById(1L)).thenReturn(sampleRoom);
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any(), eq(BookingStatus.CANCELLED)))
                .thenReturn(Collections.emptyList());
        doNothing().when(roomClientDelegate).updateRoomStatus(eq(1L), any());
        when(bookingRepository.save(any(Booking.class))).thenReturn(sampleBooking);

        BookingResponse response = bookingService.createBooking(sampleRequest);

        assertNotNull(response);
        assertEquals("HMS-BK-TEST1234", response.getBookingReference());
        assertEquals(BookingStatus.PENDING_PAYMENT, response.getStatus());
        assertEquals(PaymentStatus.UNPAID, response.getPaymentStatus());
        assertEquals(new BigDecimal("15000.00"), response.getTotalAmount());

        verify(customerClientDelegate).validateCustomerActive(1L);
        verify(roomClientDelegate).getRoomById(1L);
        verify(roomClientDelegate).updateRoomStatus(eq(1L), any(RoomStatusUpdateDto.class));
        verify(bookingRepository).save(any(Booking.class));
        verify(eventPublisher).publishBookingCreated(any(BookingCreatedEvent.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if check-out date is not after check-in date")
    void testCreateBooking_InvalidDates_ThrowsBadRequest() {
        sampleRequest.setCheckOutDate(sampleRequest.getCheckInDate());

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(sampleRequest));
        verifyNoInteractions(customerClientDelegate, roomClientDelegate, bookingRepository, eventPublisher);
    }

    @Test
    @DisplayName("Should throw BadRequestException if customer is inactive or not found")
    void testCreateBooking_InactiveCustomer_ThrowsBadRequest() {
        when(customerClientDelegate.validateCustomerActive(1L)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(sampleRequest));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException if guest count exceeds room capacity")
    void testCreateBooking_ExceedsCapacity_ThrowsBadRequest() {
        when(customerClientDelegate.validateCustomerActive(1L)).thenReturn(true);
        when(roomClientDelegate.getRoomById(1L)).thenReturn(sampleRoom);
        sampleRequest.setNumberOfGuests(4); // room capacity is 2

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(sampleRequest));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BookingConflictException if room has overlapping booking")
    void testCreateBooking_DateConflict_ThrowsBookingConflictException() {
        when(customerClientDelegate.validateCustomerActive(1L)).thenReturn(true);
        when(roomClientDelegate.getRoomById(1L)).thenReturn(sampleRoom);
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any(), eq(BookingStatus.CANCELLED)))
                .thenReturn(List.of(sampleBooking)); // conflicting booking exists

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(sampleRequest));
        verify(bookingRepository, never()).save(any());
        verify(roomClientDelegate, never()).updateRoomStatus(any(), any());
    }

    @Test
    @DisplayName("Should confirm payment and publish BookingConfirmedEvent")
    void testConfirmBookingPayment_Success() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(sampleBooking);

        BookingResponse response = bookingService.confirmBookingPayment(100L);

        assertNotNull(response);
        assertEquals(BookingStatus.CONFIRMED, sampleBooking.getStatus());
        assertEquals(PaymentStatus.PAID, sampleBooking.getPaymentStatus());
        assertNull(sampleBooking.getHoldExpiresAt());

        verify(eventPublisher).publishBookingConfirmed(any(BookingConfirmedEvent.class));
    }

    @Test
    @DisplayName("Should cancel booking, release room hold, and publish BookingCancelledEvent")
    void testCancelBooking_Success() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(sampleBooking);

        BookingResponse response = bookingService.cancelBooking(100L, "Customer requested cancellation");

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, sampleBooking.getStatus());

        verify(roomClientDelegate).updateRoomStatus(eq(1L), argThat(dto -> "AVAILABLE".equals(dto.getNewStatus())));
        verify(eventPublisher).publishBookingCancelled(any(BookingCancelledEvent.class));
    }

    @Test
    @DisplayName("Should expire unpaid holds and release room reservations")
    void testExpireUnpaidHolds() {
        when(bookingRepository.findExpiredHolds(eq(BookingStatus.PENDING_PAYMENT), any()))
                .thenReturn(List.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(sampleBooking);

        int count = bookingService.expireUnpaidHolds();

        assertEquals(1, count);
        assertEquals(BookingStatus.CANCELLED, sampleBooking.getStatus());
        verify(roomClientDelegate).updateRoomStatus(eq(1L), argThat(dto -> "AVAILABLE".equals(dto.getNewStatus())));
        verify(eventPublisher).publishBookingCancelled(any(BookingCancelledEvent.class));
    }
}
