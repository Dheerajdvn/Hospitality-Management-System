package com.hospitality.billing.dto;

import com.hospitality.billing.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Optional payment instrument fields for simulation
    private String cardNumber;
    private String cvv;
    private String expiryDate;
    private String upiId;
    private String bankName;
    private String discountCode; // e.g. "WELCOME10" for 10% discount
}
