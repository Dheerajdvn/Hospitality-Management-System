package com.hospitality.booking.repository;

import com.hospitality.booking.entity.Booking;
import com.hospitality.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findByHotelIdOrderByCreatedAtDesc(Long hotelId);

    List<Booking> findByStatus(BookingStatus status);

    /**
     * Checks for any overlapping reservations for the specified room that are not cancelled.
     * Overlap occurs when: existing.checkIn < new.checkOut AND existing.checkOut > new.checkIn
     */
    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId " +
           "AND b.status != :excludeStatus " +
           "AND b.checkInDate < :checkOutDate " +
           "AND b.checkOutDate > :checkInDate")
    List<Booking> findConflictingBookings(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("excludeStatus") BookingStatus excludeStatus);

    /**
     * Serializes concurrent booking attempts for the same room in PostgreSQL
     * using transaction-scoped advisory lock. Automatically released on commit/rollback.
     */
    @Query(value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(:roomId)) as lock_alias", nativeQuery = true)
    Integer acquireRoomAdvisoryLock(@Param("roomId") Long roomId);

    /**
     * Finds bookings whose temporary hold has expired (e.g. status = PENDING_PAYMENT and holdExpiresAt <= now).
     */
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.holdExpiresAt <= :now")
    List<Booking> findExpiredHolds(
            @Param("status") BookingStatus status,
            @Param("now") LocalDateTime now);
}
