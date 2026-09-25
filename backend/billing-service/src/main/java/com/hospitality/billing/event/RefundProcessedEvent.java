package com.hospitality.billing.event;

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
public class RefundProcessedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long billId;
    private String invoiceNumber;
    private String transactionReference;
    private Long bookingId;
    private String bookingReference;
    private Long customerId;
    private Long hotelId;
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime refundedAt;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
