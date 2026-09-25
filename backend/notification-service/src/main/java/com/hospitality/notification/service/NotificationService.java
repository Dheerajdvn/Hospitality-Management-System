package com.hospitality.notification.service;

import com.hospitality.notification.dto.DirectNotificationRequest;
import com.hospitality.notification.dto.NotificationResponse;

import java.math.BigDecimal;
import java.util.List;

public interface NotificationService {

    NotificationResponse sendDirectNotification(DirectNotificationRequest request);

    void handleBookingCreated(String bookingReference, Long customerId, BigDecimal amount, String expiresAt);

    void handleBookingConfirmed(String bookingReference, Long customerId, BigDecimal amount);

    void handleBookingCancelled(String bookingReference, Long customerId, String reason);

    void handleBookingExpired(String bookingReference, Long customerId, String reason);

    void handlePaymentProcessed(String invoiceNumber, String bookingReference, Long customerId, BigDecimal totalAmount, String status);

    void handleRefundProcessed(String invoiceNumber, String bookingReference, Long customerId, BigDecimal refundAmount, String reason);

    void handleRoomServiceRequested(Long requestId, String bookingReference, Long customerId, String serviceType);

    void handleInventoryLowStock(String itemCode, String itemName, int currentStock, int threshold);

    List<NotificationResponse> getNotificationsByCustomerId(Long customerId);


    NotificationResponse getNotificationById(Long id);
}
