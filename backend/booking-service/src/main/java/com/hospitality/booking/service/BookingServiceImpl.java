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
import com.hospitality.booking.event.BookingExpiredEvent;
import com.hospitality.booking.exception.BadRequestException;

import com.hospitality.booking.exception.BookingConflictException;
import com.hospitality.booking.exception.ResourceNotFoundException;
import com.hospitality.booking.publisher.BookingEventPublisher;
import com.hospitality.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerServiceClientDelegate customerClientDelegate;
    private final RoomServiceClientDelegate roomClientDelegate;
    private final BookingEventPublisher eventPublisher;

    @Value("${app.booking.hold-duration-minutes:15}")
    private int holdDurationMinutes;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        log.info("Processing reservation request for customer ID {} and room ID {}",
                request.getCustomerId(), request.getRoomId());

        // 1. Date Validation
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new BadRequestException("Check-out date must be strictly after check-in date");
        }
        int numberOfNights = (int) ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (numberOfNights < 1) {
            throw new BadRequestException("Minimum booking duration is 1 night");
        }

        // 2. Validate Customer via OpenFeign
        validateCustomerActive(request.getCustomerId());

        // 3. Validate Room via OpenFeign
        RoomDetailResponse room = fetchAndValidateRoom(request.getRoomId(), request.getNumberOfGuests());

        // 4. Concurrency & Date Overlap Check
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                request.getRoomId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                BookingStatus.CANCELLED
        );

        if (!conflicts.isEmpty()) {
            throw new BookingConflictException(String.format(
                    "Room %s is already reserved for the selected period (%s to %s). Please choose another room or different dates.",
                    room.getRoomNumber(), request.getCheckInDate(), request.getCheckOutDate()
            ));
        }

        // 5. Calculate Financials
        BigDecimal pricePerNight = room.getBasePrice();
        BigDecimal totalAmount = pricePerNight.multiply(BigDecimal.valueOf(numberOfNights));

        // 6. Generate Unique Reference and Temporary Hold Expiry
        String bookingReference = "HMS-BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(holdDurationMinutes);

        // 7. Update Room Status to BOOKED in Room Service with Circuit Breaker
        try {
            roomClientDelegate.updateRoomStatus(room.getId(), RoomStatusUpdateDto.builder()
                    .newStatus("BOOKED")
                    .changedBy("BOOKING_SERVICE")
                    .reason("Temporary hold for booking " + bookingReference)
                    .build());
        } catch (Exception ex) {
            log.error("Failed to place hold on room ID {}: {}", room.getId(), ex.getMessage());
            throw new BadRequestException("Could not place reservation hold on room: " + ex.getMessage());
        }

        // 8. Persist Booking Entity
        Booking booking = Booking.builder()
                .bookingReference(bookingReference)
                .customerId(request.getCustomerId())
                .roomId(room.getId())
                .hotelId(room.getHotelId())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .numberOfNights(numberOfNights)
                .numberOfGuests(request.getNumberOfGuests())
                .roomPricePerNight(pricePerNight)
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.UNPAID)
                .holdExpiresAt(holdExpiresAt)
                .specialRequests(request.getSpecialRequests())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created with ID {} and reference {}", savedBooking.getId(), savedBooking.getBookingReference());

        // 9. Publish BookingCreatedEvent to Kafka
        eventPublisher.publishBookingCreated(BookingCreatedEvent.builder()
                .bookingId(savedBooking.getId())
                .bookingReference(savedBooking.getBookingReference())
                .customerId(savedBooking.getCustomerId())
                .roomId(savedBooking.getRoomId())
                .hotelId(savedBooking.getHotelId())
                .checkInDate(savedBooking.getCheckInDate())
                .checkOutDate(savedBooking.getCheckOutDate())
                .totalAmount(savedBooking.getTotalAmount())
                .status(savedBooking.getStatus().name())
                .paymentStatus(savedBooking.getPaymentStatus().name())
                .holdExpiresAt(savedBooking.getHoldExpiresAt())
                .build());

        return mapToResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = findBookingByIdOrThrow(id);
        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + reference));
        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByCustomerId(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByHotelId(Long hotelId) {
        return bookingRepository.findByHotelIdOrderByCreatedAtDesc(hotelId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse updateBookingStatus(Long id, BookingStatusUpdateRequest request) {
        Booking booking = findBookingByIdOrThrow(id);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot update status of a cancelled booking");
        }

        booking.setStatus(request.getStatus());
        if (request.getPaymentStatus() != null) {
            booking.setPaymentStatus(request.getPaymentStatus());
        }
        if (request.getCancellationReason() != null) {
            booking.setCancellationReason(request.getCancellationReason());
        }

        // If status changed to CANCELLED, release room back to AVAILABLE
        if (request.getStatus() == BookingStatus.CANCELLED) {
            releaseRoomHold(booking.getRoomId(), "Booking " + booking.getBookingReference() + " manually cancelled");
            eventPublisher.publishBookingCancelled(BookingCancelledEvent.builder()
                    .bookingId(booking.getId())
                    .bookingReference(booking.getBookingReference())
                    .customerId(booking.getCustomerId())
                    .roomId(booking.getRoomId())
                    .hotelId(booking.getHotelId())
                    .cancellationReason(request.getCancellationReason() != null ? request.getCancellationReason() : "Manually cancelled")
                    .build());
        }

        Booking updated = bookingRepository.save(booking);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public BookingResponse confirmBookingPayment(Long id) {
        Booking booking = findBookingByIdOrThrow(id);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot confirm payment for a cancelled booking");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED && booking.getPaymentStatus() == PaymentStatus.PAID) {
            return mapToResponse(booking);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setHoldExpiresAt(null); // Hold converted to confirmed reservation

        Booking confirmed = bookingRepository.save(booking);
        log.info("Booking {} confirmed with successful payment", confirmed.getBookingReference());

        eventPublisher.publishBookingConfirmed(BookingConfirmedEvent.builder()
                .bookingId(confirmed.getId())
                .bookingReference(confirmed.getBookingReference())
                .customerId(confirmed.getCustomerId())
                .roomId(confirmed.getRoomId())
                .hotelId(confirmed.getHotelId())
                .totalAmount(confirmed.getTotalAmount())
                .status(confirmed.getStatus().name())
                .paymentStatus(confirmed.getPaymentStatus().name())
                .build());

        return mapToResponse(confirmed);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id, String reason) {
        Booking booking = findBookingByIdOrThrow(id);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return mapToResponse(booking);
        }
        if (booking.getStatus() == BookingStatus.CHECKED_OUT) {
            throw new BadRequestException("Cannot cancel an already completed booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);

        // If previously paid, mark for refund
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        Booking cancelled = bookingRepository.save(booking);
        releaseRoomHold(booking.getRoomId(), "Booking cancelled: " + reason);

        eventPublisher.publishBookingCancelled(BookingCancelledEvent.builder()
                .bookingId(cancelled.getId())
                .bookingReference(cancelled.getBookingReference())
                .customerId(cancelled.getCustomerId())
                .roomId(cancelled.getRoomId())
                .hotelId(cancelled.getHotelId())
                .cancellationReason(reason)
                .build());

        log.info("Booking {} cancelled successfully. Reason: {}", cancelled.getBookingReference(), reason);
        return mapToResponse(cancelled);
    }

    @Override
    @Transactional
    public int expireUnpaidHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findExpiredHolds(BookingStatus.PENDING_PAYMENT, now);

        if (expiredBookings.isEmpty()) {
            return 0;
        }

        log.info("Found {} expired booking holds. Releasing holds and cancelling reservations...", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancellationReason("Temporary " + holdDurationMinutes + "-minute hold expired due to lack of payment");
            bookingRepository.save(booking);

            releaseRoomHold(booking.getRoomId(), "Hold expired for booking " + booking.getBookingReference());

            eventPublisher.publishBookingCancelled(BookingCancelledEvent.builder()
                    .bookingId(booking.getId())
                    .bookingReference(booking.getBookingReference())
                    .customerId(booking.getCustomerId())
                    .roomId(booking.getRoomId())
                    .hotelId(booking.getHotelId())
                    .cancellationReason(booking.getCancellationReason())
                    .build());

            eventPublisher.publishBookingExpired(BookingExpiredEvent.builder()
                    .bookingId(booking.getId())
                    .bookingReference(booking.getBookingReference())
                    .customerId(booking.getCustomerId())
                    .roomId(booking.getRoomId())
                    .hotelId(booking.getHotelId())
                    .reason(booking.getCancellationReason())
                    .build());
        }


        return expiredBookings.size();
    }

    private Booking findBookingByIdOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));
    }

    private void validateCustomerActive(Long customerId) {
        boolean isActive = customerClientDelegate.validateCustomerActive(customerId);
        if (!isActive) {
            throw new BadRequestException("Customer with ID " + customerId + " does not exist or is inactive");
        }
    }

    private RoomDetailResponse fetchAndValidateRoom(Long roomId, int numberOfGuests) {
        RoomDetailResponse room = roomClientDelegate.getRoomById(roomId);
        if (numberOfGuests > room.getCapacity()) {
            throw new BadRequestException(String.format(
                    "Number of guests (%d) exceeds room capacity (%d) for room %s",
                    numberOfGuests, room.getCapacity(), room.getRoomNumber()
            ));
        }
        return room;
    }

    private void releaseRoomHold(Long roomId, String reason) {
        try {
            roomClientDelegate.updateRoomStatus(roomId, RoomStatusUpdateDto.builder()
                    .newStatus("AVAILABLE")
                    .changedBy("BOOKING_SERVICE")
                    .reason(reason)
                    .build());
        } catch (Exception ex) {
            log.warn("Non-fatal: could not release room hold for room ID {}: {}", roomId, ex.getMessage());
        }
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .customerId(booking.getCustomerId())
                .roomId(booking.getRoomId())
                .hotelId(booking.getHotelId())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numberOfNights(booking.getNumberOfNights())
                .numberOfGuests(booking.getNumberOfGuests())
                .roomPricePerNight(booking.getRoomPricePerNight())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .paymentStatus(booking.getPaymentStatus())
                .holdExpiresAt(booking.getHoldExpiresAt())
                .specialRequests(booking.getSpecialRequests())
                .cancellationReason(booking.getCancellationReason())
                .version(booking.getVersion())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
