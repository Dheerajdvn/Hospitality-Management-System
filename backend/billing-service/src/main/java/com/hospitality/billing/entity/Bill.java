package com.hospitality.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bills",
        indexes = {
                @Index(name = "idx_bills_invoice", columnList = "invoice_number", unique = true),
                @Index(name = "idx_bills_txn", columnList = "transaction_reference", unique = true),
                @Index(name = "idx_bills_booking", columnList = "booking_id"),
                @Index(name = "idx_bills_customer", columnList = "customer_id"),
                @Index(name = "idx_bills_hotel", columnList = "hotel_id"),
                @Index(name = "idx_bills_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 40)
    private String invoiceNumber;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 40)
    private String transactionReference;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "booking_reference", nullable = false, length = 32)
    private String bookingReference;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "base_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "tax_rate_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRatePercentage;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BillStatus status;

    @Column(name = "payment_gateway_response", length = 255)
    private String paymentGatewayResponse;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_reason", length = 255)
    private String refundReason;

    /**
     * Optimistic locking version field.
     * Prevents duplicate payments and concurrent modification anomalies.
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
