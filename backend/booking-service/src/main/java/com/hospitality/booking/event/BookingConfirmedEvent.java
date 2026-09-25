package com.hospitality.booking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long bookingId;
    private String bookingReference;
    private Long customerId;
    private Long roomId;
    private Long hotelId;
    private BigDecimal totalAmount;
    private String status;
    private String paymentStatus;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
