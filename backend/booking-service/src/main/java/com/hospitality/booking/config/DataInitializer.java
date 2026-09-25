package com.hospitality.booking.config;

import com.hospitality.booking.entity.Booking;
import com.hospitality.booking.entity.BookingStatus;
import com.hospitality.booking.entity.PaymentStatus;
import com.hospitality.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BookingRepository bookingRepository;

    @Override
    public void run(String... args) {
        if (bookingRepository.count() > 0) {
            log.info("Booking database already initialized. Skipping seed data.");
            return;
        }

        log.info("Seeding initial demo bookings into hms_booking_db...");

        LocalDate today = LocalDate.now();

        List<Booking> demoBookings = List.of(
                // Booking 1: Confirmed booking for Customer 1
                Booking.builder()
                        .bookingReference("HMS-BK-DEMO0001")
                        .customerId(1L)
                        .roomId(2L)
                        .hotelId(1L)
                        .checkInDate(today.plusDays(3))
                        .checkOutDate(today.plusDays(6))
                        .numberOfNights(3)
                        .numberOfGuests(2)
                        .roomPricePerNight(new BigDecimal("5500.00"))
                        .totalAmount(new BigDecimal("16500.00"))
                        .status(BookingStatus.CONFIRMED)
                        .paymentStatus(PaymentStatus.PAID)
                        .holdExpiresAt(null)
                        .specialRequests("Late check-in requested after 8:00 PM")
                        .build(),

                // Booking 2: Pending hold booking for Customer 1
                Booking.builder()
                        .bookingReference("HMS-BK-DEMO0002")
                        .customerId(1L)
                        .roomId(3L)
                        .hotelId(1L)
                        .checkInDate(today.plusDays(10))
                        .checkOutDate(today.plusDays(12))
                        .numberOfNights(2)
                        .numberOfGuests(2)
                        .roomPricePerNight(new BigDecimal("8500.00"))
                        .totalAmount(new BigDecimal("17000.00"))
                        .status(BookingStatus.PENDING_PAYMENT)
                        .paymentStatus(PaymentStatus.UNPAID)
                        .holdExpiresAt(LocalDateTime.now().plusMinutes(15))
                        .specialRequests("High floor sea view room preferred")
                        .build()
        );

        bookingRepository.saveAll(demoBookings);
        log.info("Successfully seeded 2 demo bookings into hms_booking_db.");
    }
}
