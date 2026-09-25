package com.hospitality.billing.dto;

import com.hospitality.billing.entity.BillStatus;
import com.hospitality.billing.entity.PaymentMethod;
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
public class BillResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String invoiceNumber;
    private String transactionReference;
    private Long bookingId;
    private String bookingReference;
    private Long customerId;
    private Long hotelId;
    private BigDecimal baseAmount;
    private BigDecimal taxRatePercentage;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private BillStatus status;
    private String paymentGatewayResponse;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private String refundReason;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
