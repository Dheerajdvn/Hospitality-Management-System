package com.hospitality.booking.dto;

import com.hospitality.booking.entity.BookingStatus;
import com.hospitality.booking.entity.PaymentStatus;
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
public class BookingResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String bookingReference;
    private Long customerId;
    private Long roomId;
    private Long hotelId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfNights;
    private Integer numberOfGuests;
    private BigDecimal roomPricePerNight;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime holdExpiresAt;
    private String specialRequests;
    private String cancellationReason;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
