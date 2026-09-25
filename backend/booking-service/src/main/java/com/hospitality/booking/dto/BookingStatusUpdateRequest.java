package com.hospitality.booking.dto;

import com.hospitality.booking.entity.BookingStatus;
import com.hospitality.booking.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusUpdateRequest {

    @NotNull(message = "New status is required")
    private BookingStatus status;

    private PaymentStatus paymentStatus;

    private String cancellationReason;
}
