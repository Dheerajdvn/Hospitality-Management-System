package com.hospitality.booking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long bookingId;
    private String bookingReference;
    private Long customerId;
    private Long roomId;
    private Long hotelId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalAmount;
    private String status;
    private String paymentStatus;
    private LocalDateTime holdExpiresAt;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
