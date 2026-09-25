package com.hospitality.billing.service;

import com.hospitality.billing.client.BookingServiceClientDelegate;
import com.hospitality.billing.dto.*;
import com.hospitality.billing.entity.Bill;
import com.hospitality.billing.entity.BillStatus;
import com.hospitality.billing.event.PaymentCompletedEvent;
import com.hospitality.billing.event.PaymentFailedEvent;
import com.hospitality.billing.event.PaymentProcessedEvent;
import com.hospitality.billing.event.RefundProcessedEvent;

import com.hospitality.billing.exception.BadRequestException;
import com.hospitality.billing.exception.PaymentFailedException;
import com.hospitality.billing.exception.ResourceNotFoundException;
import com.hospitality.billing.publisher.BillingEventPublisher;
import com.hospitality.billing.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final BillRepository billRepository;
    private final BookingServiceClientDelegate bookingClientDelegate;
    private final BillingEventPublisher eventPublisher;

    @Value("${app.billing.tax-rate-percentage:18.0}")
    private double defaultTaxRatePercentage;

    @Override
    @Transactional
    public BillResponse processPayment(PaymentRequest request) {
        log.info("Initiating payment processing for booking ID: {}", request.getBookingId());

        // 1. Fetch Booking via OpenFeign with Resilience4j Circuit Breaker & Retry
        BookingDetailResponse booking = bookingClientDelegate.fetchBooking(request.getBookingId());

        // 2. Validate Booking State
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new BadRequestException("Cannot process payment for cancelled booking " + booking.getBookingReference());
        }

        if (billRepository.existsByBookingIdAndStatus(request.getBookingId(), BillStatus.PAID)) {
            throw new BadRequestException("A successful payment has already been recorded for booking " + 
                    booking.getBookingReference());
        }

        // 3. Tax and Total Calculations
        BigDecimal baseAmount = booking.getTotalAmount();
        BigDecimal taxRate = BigDecimal.valueOf(defaultTaxRatePercentage);
        BigDecimal taxAmount = baseAmount.multiply(taxRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Optional promotional discount simulation
        BigDecimal discountAmount = BigDecimal.ZERO;
        if ("WELCOME10".equalsIgnoreCase(request.getDiscountCode())) {
            discountAmount = baseAmount.multiply(new BigDecimal("0.10"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAmount = baseAmount.add(taxAmount).subtract(discountAmount);

        // 4. Identifiers Generation
        String invoiceNumber = "INV-" + Year.now().getValue() + "-" + 
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String transactionReference = "TXN-" + 
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 5. Payment Simulation Check
        boolean isSimulatedFailure = isSimulationFailureTriggered(request);

        if (isSimulatedFailure) {
            log.warn("Simulated payment gateway triggered failure for booking ID: {}", request.getBookingId());
            Bill failedBill = Bill.builder()
                    .invoiceNumber(invoiceNumber)
                    .transactionReference(transactionReference)
                    .bookingId(booking.getId())
                    .bookingReference(booking.getBookingReference())
                    .customerId(booking.getCustomerId())
                    .hotelId(booking.getHotelId())
                    .baseAmount(baseAmount)
                    .taxRatePercentage(taxRate)
                    .taxAmount(taxAmount)
                    .discountAmount(discountAmount)
                    .totalAmount(totalAmount)
                    .paymentMethod(request.getPaymentMethod())
                    .status(BillStatus.FAILED)
                    .paymentGatewayResponse("DECLINED: Transaction rejected by issuer (simulated card failure)")
                    .build();

            Bill savedFailedBill = billRepository.save(failedBill);

            eventPublisher.publishPaymentProcessed(PaymentProcessedEvent.builder()
                    .billId(savedFailedBill.getId())
                    .invoiceNumber(savedFailedBill.getInvoiceNumber())
                    .transactionReference(savedFailedBill.getTransactionReference())
                    .bookingId(savedFailedBill.getBookingId())
                    .bookingReference(savedFailedBill.getBookingReference())
                    .customerId(savedFailedBill.getCustomerId())
                    .hotelId(savedFailedBill.getHotelId())
                    .totalAmount(savedFailedBill.getTotalAmount())
                    .paymentMethod(savedFailedBill.getPaymentMethod().name())
                    .status("FAILED")
                    .paidAt(null)
                    .build());

            eventPublisher.publishPaymentFailed(PaymentFailedEvent.builder()
                    .billId(savedFailedBill.getId())
                    .invoiceNumber(savedFailedBill.getInvoiceNumber())
                    .transactionReference(savedFailedBill.getTransactionReference())
                    .bookingId(savedFailedBill.getBookingId())
                    .bookingReference(savedFailedBill.getBookingReference())
                    .customerId(savedFailedBill.getCustomerId())
                    .hotelId(savedFailedBill.getHotelId())
                    .totalAmount(savedFailedBill.getTotalAmount())
                    .paymentMethod(savedFailedBill.getPaymentMethod().name())
                    .status("FAILED")
                    .failureReason("Declined by simulated payment gateway (card simulation)")
                    .build());

            throw new PaymentFailedException("Payment declined by payment gateway simulation. Please try a different card.");
        }

        // 6. Success Scenario
        String authCode = "AUTH-" + (100000 + new Random().nextInt(900000));
        LocalDateTime now = LocalDateTime.now();

        Bill bill = Bill.builder()
                .invoiceNumber(invoiceNumber)
                .transactionReference(transactionReference)
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .customerId(booking.getCustomerId())
                .hotelId(booking.getHotelId())
                .baseAmount(baseAmount)
                .taxRatePercentage(taxRate)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .status(BillStatus.PAID)
                .paymentGatewayResponse("SUCCESS - Authorization: " + authCode)
                .paidAt(now)
                .build();

        Bill savedBill = billRepository.save(bill);

        // 7. Inter-service confirmation: Notify booking-service with Circuit Breaker
        try {
            bookingClientDelegate.confirmBookingPayment(savedBill.getBookingId());
            log.info("Notified booking-service for confirmation of booking ID: {}", savedBill.getBookingId());
        } catch (Exception ex) {
            log.error("Failed to notify booking-service of payment confirmation: {}", ex.getMessage());
            // Non-fatal for payment recording, but logged for reconciliation
        }

        // 8. Publish Kafka Events (Both topic schemas supported)
        eventPublisher.publishPaymentProcessed(PaymentProcessedEvent.builder()
                .billId(savedBill.getId())
                .invoiceNumber(savedBill.getInvoiceNumber())
                .transactionReference(savedBill.getTransactionReference())
                .bookingId(savedBill.getBookingId())
                .bookingReference(savedBill.getBookingReference())
                .customerId(savedBill.getCustomerId())
                .hotelId(savedBill.getHotelId())
                .totalAmount(savedBill.getTotalAmount())
                .paymentMethod(savedBill.getPaymentMethod().name())
                .status("SUCCESS")
                .paidAt(savedBill.getPaidAt())
                .build());

        eventPublisher.publishPaymentCompleted(PaymentCompletedEvent.builder()
                .billId(savedBill.getId())
                .invoiceNumber(savedBill.getInvoiceNumber())
                .transactionReference(savedBill.getTransactionReference())
                .bookingId(savedBill.getBookingId())
                .bookingReference(savedBill.getBookingReference())
                .customerId(savedBill.getCustomerId())
                .hotelId(savedBill.getHotelId())
                .totalAmount(savedBill.getTotalAmount())
                .paymentMethod(savedBill.getPaymentMethod().name())
                .status("SUCCESS")
                .paidAt(savedBill.getPaidAt())
                .build());

        log.info("Payment processed successfully. Invoice: {}, Txn: {}", invoiceNumber, transactionReference);
        return mapToResponse(savedBill);

    }

    @Override
    @Transactional(readOnly = true)
    public BillResponse getBillById(Long id) {
        Bill bill = findBillByIdOrThrow(id);
        return mapToResponse(bill);
    }

    @Override
    @Transactional(readOnly = true)
    public BillResponse getBillByInvoiceNumber(String invoiceNumber) {
        Bill bill = billRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with invoice number: " + invoiceNumber));
        return mapToResponse(bill);
    }

    @Override
    @Transactional(readOnly = true)
    public BillResponse getBillByBookingId(Long bookingId) {
        Bill bill = billRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No bill found for booking ID: " + bookingId));
        return mapToResponse(bill);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillResponse> getBillsByCustomerId(Long customerId) {
        return billRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillResponse> getBillsByHotelId(Long hotelId) {
        return billRepository.findByHotelIdOrderByCreatedAtDesc(hotelId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long billId) {
        Bill bill = findBillByIdOrThrow(billId);

        BookingDetailResponse booking = null;
        try {
            booking = bookingClientDelegate.fetchBooking(bill.getBookingId());
        } catch (Exception ex) {
            log.warn("Could not fetch booking details for invoice (circuit breaker or downstream error): {}", ex.getMessage());
        }

        BigDecimal halfTax = bill.getTaxAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        return InvoiceResponse.builder()
                .invoiceNumber(bill.getInvoiceNumber())
                .transactionReference(bill.getTransactionReference())
                .invoiceDate(bill.getPaidAt() != null ? bill.getPaidAt() : bill.getCreatedAt())
                .hotelName("HMS Partner Hotel #" + bill.getHotelId())
                .hotelAddress("Verified Hospitality Property")
                .customerId(bill.getCustomerId())
                .bookingReference(bill.getBookingReference())
                .numberOfNights(booking != null ? booking.getNumberOfNights() : 1)
                .roomPricePerNight(booking != null ? booking.getRoomPricePerNight() : bill.getBaseAmount())
                .subTotal(bill.getBaseAmount())
                .cgstAmount(halfTax)
                .sgstAmount(halfTax)
                .totalTaxAmount(bill.getTaxAmount())
                .discountAmount(bill.getDiscountAmount())
                .grandTotal(bill.getTotalAmount())
                .paymentMethod(bill.getPaymentMethod().name())
                .paymentStatus(bill.getStatus().name())
                .paidAt(bill.getPaidAt())
                .authorizationCode(bill.getPaymentGatewayResponse())
                .build();
    }

    @Override
    @Transactional
    public BillResponse processRefund(Long billId, String reason) {
        Bill bill = findBillByIdOrThrow(billId);

        if (bill.getStatus() != BillStatus.PAID) {
            throw new BadRequestException("Cannot refund a bill with status " + bill.getStatus());
        }

        bill.setStatus(BillStatus.REFUNDED);
        bill.setRefundedAt(LocalDateTime.now());
        bill.setRefundReason(reason != null ? reason : "Booking cancellation refund");

        Bill refunded = billRepository.save(bill);

        eventPublisher.publishRefundProcessed(RefundProcessedEvent.builder()
                .billId(refunded.getId())
                .invoiceNumber(refunded.getInvoiceNumber())
                .transactionReference(refunded.getTransactionReference())
                .bookingId(refunded.getBookingId())
                .bookingReference(refunded.getBookingReference())
                .customerId(refunded.getCustomerId())
                .hotelId(refunded.getHotelId())
                .refundAmount(refunded.getTotalAmount())
                .refundReason(refunded.getRefundReason())
                .refundedAt(refunded.getRefundedAt())
                .build());

        log.info("Bill ID {} refunded successfully for amount ₹{}", billId, refunded.getTotalAmount());
        return mapToResponse(refunded);
    }

    private Bill findBillByIdOrThrow(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + id));
    }

    private boolean isSimulationFailureTriggered(PaymentRequest request) {
        if (request.getCardNumber() != null && request.getCardNumber().endsWith("9999")) {
            return true;
        }
        if (request.getUpiId() != null && request.getUpiId().toLowerCase().contains("fail")) {
            return true;
        }
        return false;
    }

    private BillResponse mapToResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .invoiceNumber(bill.getInvoiceNumber())
                .transactionReference(bill.getTransactionReference())
                .bookingId(bill.getBookingId())
                .bookingReference(bill.getBookingReference())
                .customerId(bill.getCustomerId())
                .hotelId(bill.getHotelId())
                .baseAmount(bill.getBaseAmount())
                .taxRatePercentage(bill.getTaxRatePercentage())
                .taxAmount(bill.getTaxAmount())
                .discountAmount(bill.getDiscountAmount())
                .totalAmount(bill.getTotalAmount())
                .paymentMethod(bill.getPaymentMethod())
                .status(bill.getStatus())
                .paymentGatewayResponse(bill.getPaymentGatewayResponse())
                .paidAt(bill.getPaidAt())
                .refundedAt(bill.getRefundedAt())
                .refundReason(bill.getRefundReason())
                .version(bill.getVersion())
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                .build();
    }
}
