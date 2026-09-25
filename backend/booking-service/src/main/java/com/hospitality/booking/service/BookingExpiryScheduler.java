package com.hospitality.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingService bookingService;

    /**
     * Periodically inspects active booking holds every 60 seconds.
     * Automatically cancels bookings whose 15-minute payment window has elapsed.
     */
    @Scheduled(fixedRate = 60000)
    public void scheduleExpiredHoldCleanup() {
        try {
            int expiredCount = bookingService.expireUnpaidHolds();
            if (expiredCount > 0) {
                log.info("Expired and released {} unpaid booking holds.", expiredCount);
            }
        } catch (Exception ex) {
            log.error("Error during scheduled booking hold expiry check: {}", ex.getMessage());
        }
    }
}
