package com.hospitality.billing.service;

import com.hospitality.billing.client.BookingServiceClientDelegate;
import com.hospitality.billing.dto.*;
import com.hospitality.billing.entity.Bill;
import com.hospitality.billing.entity.BillStatus;
import com.hospitality.billing.entity.PaymentMethod;
import com.hospitality.billing.event.PaymentProcessedEvent;
import com.hospitality.billing.event.RefundProcessedEvent;
import com.hospitality.billing.exception.BadRequestException;
import com.hospitality.billing.exception.PaymentFailedException;
import com.hospitality.billing.publisher.BillingEventPublisher;
import com.hospitality.billing.repository.BillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private BillRepository billRepository;

    @Mock
    private BookingServiceClientDelegate bookingClientDelegate;

    @Mock
    private BillingEventPublisher eventPublisher;

    @InjectMocks
    private BillingServiceImpl billingService;

    private BookingDetailResponse sampleBooking;
    private PaymentRequest sampleRequest;
    private Bill sampleBill;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(billingService, "defaultTaxRatePercentage", 18.0);

        sampleBooking = BookingDetailResponse.builder()
                .id(1L)
                .bookingReference("HMS-BK-TEST001")
                .customerId(1L)
                .roomId(2L)
                .hotelId(1L)
                .checkInDate(LocalDate.now().plusDays(2))
                .checkOutDate(LocalDate.now().plusDays(5))
                .numberOfNights(3)
                .numberOfGuests(2)
                .roomPricePerNight(new BigDecimal("5000.00"))
                .totalAmount(new BigDecimal("15000.00"))
                .status("PENDING_PAYMENT")
                .paymentStatus("UNPAID")
                .build();

        sampleRequest = PaymentRequest.builder()
                .bookingId(1L)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111111111111234")
                .cvv("123")
                .expiryDate("12/28")
                .build();

        sampleBill = Bill.builder()
                .id(10L)
                .invoiceNumber("INV-2026-TEST001")
                .transactionReference("TXN-TEST001")
                .bookingId(1L)
                .bookingReference("HMS-BK-TEST001")
                .customerId(1L)
                .hotelId(1L)
                .baseAmount(new BigDecimal("15000.00"))
                .taxRatePercentage(new BigDecimal("18.00"))
                .taxAmount(new BigDecimal("2700.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("17700.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(BillStatus.PAID)
                .paymentGatewayResponse("SUCCESS - Authorization: AUTH-123456")
                .paidAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully process payment, calculate 18% GST, notify booking service via resilient delegate, and publish Kafka event")
    void testProcessPayment_Success() {
        when(bookingClientDelegate.fetchBooking(1L)).thenReturn(sampleBooking);
        when(billRepository.existsByBookingIdAndStatus(1L, BillStatus.PAID)).thenReturn(false);
        when(billRepository.save(any(Bill.class))).thenReturn(sampleBill);
        doNothing().when(bookingClientDelegate).confirmBookingPayment(1L);

        BillResponse response = billingService.processPayment(sampleRequest);

        assertNotNull(response);
        assertEquals("INV-2026-TEST001", response.getInvoiceNumber());
        assertEquals(BillStatus.PAID, response.getStatus());
        assertEquals(new BigDecimal("17700.00"), response.getTotalAmount());

        verify(bookingClientDelegate).fetchBooking(1L);
        verify(bookingClientDelegate).confirmBookingPayment(1L);
        verify(billRepository).save(any(Bill.class));
        verify(eventPublisher).publishPaymentProcessed(any(PaymentProcessedEvent.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if booking is cancelled")
    void testProcessPayment_CancelledBooking_ThrowsBadRequest() {
        sampleBooking.setStatus("CANCELLED");
        when(bookingClientDelegate.fetchBooking(1L)).thenReturn(sampleBooking);

        assertThrows(BadRequestException.class, () -> billingService.processPayment(sampleRequest));
        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException if booking is already paid")
    void testProcessPayment_AlreadyPaid_ThrowsBadRequest() {
        when(bookingClientDelegate.fetchBooking(1L)).thenReturn(sampleBooking);
        when(billRepository.existsByBookingIdAndStatus(1L, BillStatus.PAID)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> billingService.processPayment(sampleRequest));
        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should trigger simulated payment decline when card ends with 9999")
    void testProcessPayment_SimulationFailure() {
        sampleRequest.setCardNumber("4111111111119999");
        when(bookingClientDelegate.fetchBooking(1L)).thenReturn(sampleBooking);
        when(billRepository.existsByBookingIdAndStatus(1L, BillStatus.PAID)).thenReturn(false);

        Bill failedBill = Bill.builder()
                .id(20L)
                .invoiceNumber("INV-FAIL")
                .transactionReference("TXN-FAIL")
                .bookingId(1L)
                .bookingReference("HMS-BK-TEST001")
                .customerId(1L)
                .hotelId(1L)
                .totalAmount(new BigDecimal("17700.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(BillStatus.FAILED)
                .build();
        when(billRepository.save(any(Bill.class))).thenReturn(failedBill);

        assertThrows(PaymentFailedException.class, () -> billingService.processPayment(sampleRequest));

        verify(bookingClientDelegate, never()).confirmBookingPayment(any());
        verify(eventPublisher).publishPaymentProcessed(argThat(event -> "FAILED".equals(event.getStatus())));
    }

    @Test
    @DisplayName("Should calculate 10% discount when discount code WELCOME10 is provided")
    void testProcessPayment_WithDiscount() {
        sampleRequest.setDiscountCode("WELCOME10");
        when(bookingClientDelegate.fetchBooking(1L)).thenReturn(sampleBooking);
        when(billRepository.existsByBookingIdAndStatus(1L, BillStatus.PAID)).thenReturn(false);
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillResponse response = billingService.processPayment(sampleRequest);

        assertNotNull(response);
        // Base = 15,000. Discount = 1,500. Tax (18% on 15,000) = 2,700. Total = 15,000 + 2,700 - 1,500 = 16,200.
        assertEquals(new BigDecimal("1500.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("16200.00"), response.getTotalAmount());
    }

    @Test
    @DisplayName("Should successfully process refund for paid bill and publish RefundProcessedEvent")
    void testProcessRefund_Success() {
        when(billRepository.findById(10L)).thenReturn(Optional.of(sampleBill));
        when(billRepository.save(any(Bill.class))).thenReturn(sampleBill);

        BillResponse response = billingService.processRefund(10L, "Customer cancellation");

        assertNotNull(response);
        assertEquals(BillStatus.REFUNDED, sampleBill.getStatus());
        assertNotNull(sampleBill.getRefundedAt());
        assertEquals("Customer cancellation", sampleBill.getRefundReason());

        verify(eventPublisher).publishRefundProcessed(any(RefundProcessedEvent.class));
    }

    @Test
    @DisplayName("Should generate rich tax invoice with split CGST and SGST")
    void testGetInvoice_Success() {
        when(billRepository.findById(10L)).thenReturn(Optional.of(sampleBill));
        when(bookingClientDelegate.fetchBooking(1L)).thenReturn(sampleBooking);

        InvoiceResponse invoice = billingService.getInvoice(10L);

        assertNotNull(invoice);
        assertEquals("INV-2026-TEST001", invoice.getInvoiceNumber());
        assertEquals(new BigDecimal("15000.00"), invoice.getSubTotal());
        assertEquals(new BigDecimal("1350.00"), invoice.getCgstAmount()); // 2700 / 2
        assertEquals(new BigDecimal("1350.00"), invoice.getSgstAmount()); // 2700 / 2
        assertEquals(new BigDecimal("2700.00"), invoice.getTotalTaxAmount());
        assertEquals(new BigDecimal("17700.00"), invoice.getGrandTotal());
    }
}
