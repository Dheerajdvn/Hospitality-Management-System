package com.hospitality.booking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingExpiredEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long bookingId;
    private String bookingReference;
    private Long customerId;
    private Long roomId;
    private Long hotelId;
    private String reason;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
