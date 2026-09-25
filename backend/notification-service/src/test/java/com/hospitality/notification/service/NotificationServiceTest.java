package com.hospitality.notification.service;

import com.hospitality.notification.dispatcher.SimulatedEmailDispatcher;
import com.hospitality.notification.dispatcher.SimulatedSmsDispatcher;
import com.hospitality.notification.dto.DirectNotificationRequest;
import com.hospitality.notification.dto.NotificationResponse;
import com.hospitality.notification.entity.NotificationChannel;
import com.hospitality.notification.entity.NotificationLog;
import com.hospitality.notification.entity.NotificationStatus;
import com.hospitality.notification.entity.NotificationType;
import com.hospitality.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private SimulatedEmailDispatcher emailDispatcher;

    @Mock
    private SimulatedSmsDispatcher smsDispatcher;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        lenient().when(emailDispatcher.sendEmail(any(), any(), any())).thenReturn(true);
        lenient().when(smsDispatcher.sendSms(any(), any())).thenReturn(true);
        lenient().when(logRepository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should send direct email notification and save audit log with status SENT")
    void testSendDirectEmail_Success() {
        DirectNotificationRequest request = DirectNotificationRequest.builder()
                .customerId(1L)
                .recipient("john.doe@example.com")
                .channel(NotificationChannel.EMAIL)
                .subject("Important Account Update")
                .content("Your password was updated.")
                .build();

        NotificationResponse response = notificationService.sendDirectNotification(request);

        assertNotNull(response);
        assertEquals("john.doe@example.com", response.getRecipient());
        assertEquals(NotificationStatus.SENT, response.getStatus());

        verify(emailDispatcher).sendEmail(eq("john.doe@example.com"), eq("Important Account Update"), any());
        verify(logRepository).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Should send direct SMS alert and save audit log")
    void testSendDirectSms_Success() {
        DirectNotificationRequest request = DirectNotificationRequest.builder()
                .customerId(1L)
                .recipient("+91-9876543210")
                .channel(NotificationChannel.SMS)
                .subject("SMS Alert")
                .content("Your room key is ready at reception.")
                .build();

        NotificationResponse response = notificationService.sendDirectNotification(request);

        assertNotNull(response);
        assertEquals("+91-9876543210", response.getRecipient());
        assertEquals(NotificationStatus.SENT, response.getStatus());

        verify(smsDispatcher).sendSms(eq("+91-9876543210"), eq("Your room key is ready at reception."));
        verify(logRepository).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Should handle booking created event by dispatching email and SMS")
    void testHandleBookingCreated() {
        notificationService.handleBookingCreated("HMS-BK-1234", 1L, new BigDecimal("15000.00"), "15 mins from now");

        verify(emailDispatcher).sendEmail(contains("customer1"), contains("HMS-BK-1234"), any());
        verify(smsDispatcher).sendSms(contains("+91"), contains("HMS-BK-1234"));
        verify(logRepository, times(2)).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Should handle booking confirmed event with voucher dispatch")
    void testHandleBookingConfirmed() {
        notificationService.handleBookingConfirmed("HMS-BK-5678", 2L, new BigDecimal("25000.00"));

        verify(emailDispatcher).sendEmail(contains("customer2"), contains("Stay Voucher"), any());
        verify(smsDispatcher).sendSms(contains("+91"), contains("CONFIRMED"));
        verify(logRepository, times(2)).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Should handle booking cancelled event with alert dispatch")
    void testHandleBookingCancelled() {
        notificationService.handleBookingCancelled("HMS-BK-9999", 3L, "Travel changed");

        verify(emailDispatcher).sendEmail(contains("customer3"), contains("Cancelled"), any());
        verify(smsDispatcher).sendSms(contains("+91"), contains("cancelled"));
        verify(logRepository, times(2)).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Should handle payment processed event with tax receipt dispatch")
    void testHandlePaymentProcessed() {
        notificationService.handlePaymentProcessed("INV-2026-001", "HMS-BK-001", 1L, new BigDecimal("17700.00"), "SUCCESS");

        verify(emailDispatcher).sendEmail(contains("customer1"), contains("INV-2026-001"), any());
        verify(logRepository, times(1)).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Should handle refund processed event with email and SMS")
    void testHandleRefundProcessed() {
        notificationService.handleRefundProcessed("INV-2026-001", "HMS-BK-001", 1L, new BigDecimal("17700.00"), "Cancellation refund");

        verify(emailDispatcher).sendEmail(contains("customer1"), contains("Refund Confirmation"), any());
        verify(smsDispatcher).sendSms(contains("+91"), contains("Refund of Rs. 17700.00"));
        verify(logRepository, times(2)).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Should retrieve notification history by customer ID")
    void testGetNotificationsByCustomerId() {
        NotificationLog mockLog = NotificationLog.builder()
                .id(1L)
                .customerId(1L)
                .recipient("test@example.com")
                .channel(NotificationChannel.EMAIL)
                .type(NotificationType.BOOKING_CONFIRMED)
                .subject("Booking Confirmed")
                .content("Details")
                .status(NotificationStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();

        when(logRepository.findByCustomerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(mockLog));

        List<NotificationResponse> list = notificationService.getNotificationsByCustomerId(1L);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("test@example.com", list.get(0).getRecipient());
    }
}
