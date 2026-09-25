package com.hospitality.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_ref", columnList = "booking_reference", unique = true),
                @Index(name = "idx_bookings_customer", columnList = "customer_id"),
                @Index(name = "idx_bookings_room", columnList = "room_id"),
                @Index(name = "idx_bookings_hotel", columnList = "hotel_id"),
                @Index(name = "idx_bookings_status", columnList = "status"),
                @Index(name = "idx_bookings_dates", columnList = "room_id, check_in_date, check_out_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, unique = true, length = 32)
    private String bookingReference;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "number_of_nights", nullable = false)
    private Integer numberOfNights;

    @Column(name = "number_of_guests", nullable = false)
    private Integer numberOfGuests;

    @Column(name = "room_price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal roomPricePerNight;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "special_requests", length = 500)
    private String specialRequests;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    /**
     * Optimistic locking version field.
     * Prevents race conditions and lost updates during state transitions.
     */
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
