package com.hospitality.billing.dto;

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
public class InvoiceResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    // Header & Meta
    private String invoiceNumber;
    private String transactionReference;
    private LocalDateTime invoiceDate;
    private String hotelName;
    private String hotelAddress;

    // Customer Info
    private Long customerId;
    private String bookingReference;

    // Line items & calculations
    private Integer numberOfNights;
    private BigDecimal roomPricePerNight;
    private BigDecimal subTotal;
    private BigDecimal cgstAmount; // 9%
    private BigDecimal sgstAmount; // 9%
    private BigDecimal totalTaxAmount; // 18%
    private BigDecimal discountAmount;
    private BigDecimal grandTotal;

    // Payment state
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime paidAt;
    private String authorizationCode;
}
