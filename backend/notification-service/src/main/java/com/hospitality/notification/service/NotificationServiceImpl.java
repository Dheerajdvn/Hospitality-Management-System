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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogRepository logRepository;
    private final SimulatedEmailDispatcher emailDispatcher;
    private final SimulatedSmsDispatcher smsDispatcher;

    @Override
    @Transactional
    public NotificationResponse sendDirectNotification(DirectNotificationRequest request) {
        log.info("Sending direct {} notification to {}", request.getChannel(), request.getRecipient());

        boolean success;
        if (request.getChannel() == NotificationChannel.EMAIL) {
            success = emailDispatcher.sendEmail(request.getRecipient(), request.getSubject(), request.getContent());
        } else {
            success = smsDispatcher.sendSms(request.getRecipient(), request.getContent());
        }

        NotificationLog notificationLog = NotificationLog.builder()
                .customerId(request.getCustomerId())
                .recipient(request.getRecipient())
                .channel(request.getChannel())
                .type(request.getType() != null ? request.getType() : NotificationType.DIRECT_ALERT)
                .referenceId(request.getReferenceId())
                .subject(request.getSubject())
                .content(request.getContent())
                .status(success ? NotificationStatus.SENT : NotificationStatus.FAILED)
                .errorMessage(success ? null : "Simulated dispatcher delivery failure")
                .sentAt(success ? LocalDateTime.now() : null)
                .build();

        NotificationLog saved = logRepository.save(notificationLog);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void handleBookingCreated(String bookingReference, Long customerId, BigDecimal amount, String expiresAt) {
        log.info("Handling booking created notification for reference: {}", bookingReference);
        String recipientEmail = "customer" + customerId + "@hms-guest.com";
        String recipientPhone = "+91-9876543210";

        String emailSubject = "Room Reservation Hold Placed - " + bookingReference;
        String emailBody = String.format(
                "<h2>Hospitality Management System</h2>" +
                "<p>Dear Guest,</p>" +
                "<p>Your room reservation hold has been placed for reference: <b>%s</b>.</p>" +
                "<p>Total Amount: <b>₹%s</b></p>" +
                "<p style='color:red;'>Please complete your payment before <b>%s</b> (15-minute temporary hold).</p>",
                bookingReference, amount, expiresAt
        );

        String smsBody = String.format(
                "HMS Alert: Hold placed for booking %s. Total: Rs. %s. Pay within 15 mins to confirm reservation.",
                bookingReference, amount
        );

        dispatchAndLog(customerId, recipientEmail, NotificationChannel.EMAIL, NotificationType.BOOKING_HOLD,
                bookingReference, emailSubject, emailBody);
        dispatchAndLog(customerId, recipientPhone, NotificationChannel.SMS, NotificationType.BOOKING_HOLD,
                bookingReference, "Reservation Hold", smsBody);
    }

    @Override
    @Transactional
    public void handleBookingConfirmed(String bookingReference, Long customerId, BigDecimal amount) {
        log.info("Handling booking confirmed notification for reference: {}", bookingReference);
        String recipientEmail = "customer" + customerId + "@hms-guest.com";
        String recipientPhone = "+91-9876543210";

        String emailSubject = "Booking Confirmed! Your Stay Voucher - " + bookingReference;
        String emailBody = String.format(
                "<h2>Reservation Confirmed!</h2>" +
                "<p>Dear Guest,</p>" +
                "<p>We look forward to welcoming you! Your reservation <b>%s</b> is now officially confirmed.</p>" +
                "<p>Total Paid: <b>₹%s</b></p>" +
                "<p>Check-in time starts at 2:00 PM. Please present your booking reference at the front desk.</p>",
                bookingReference, amount
        );

        String smsBody = String.format(
                "HMS: Your reservation %s is CONFIRMED. Total: Rs. %s. Show this reference at hotel check-in.",
                bookingReference, amount
        );

        dispatchAndLog(customerId, recipientEmail, NotificationChannel.EMAIL, NotificationType.BOOKING_CONFIRMED,
                bookingReference, emailSubject, emailBody);
        dispatchAndLog(customerId, recipientPhone, NotificationChannel.SMS, NotificationType.BOOKING_CONFIRMED,
                bookingReference, "Booking Confirmed", smsBody);
    }

    @Override
    @Transactional
    public void handleBookingCancelled(String bookingReference, Long customerId, String reason) {
        log.info("Handling booking cancelled notification for reference: {}", bookingReference);
        String recipientEmail = "customer" + customerId + "@hms-guest.com";
        String recipientPhone = "+91-9876543210";

        String emailSubject = "Booking Cancelled - " + bookingReference;
        String emailBody = String.format(
                "<h2>Booking Cancellation Notice</h2>" +
                "<p>Dear Guest,</p>" +
                "<p>Your reservation <b>%s</b> has been cancelled.</p>" +
                "<p>Reason: <i>%s</i></p>" +
                "<p>Any eligible refund will be credited back to your original payment method within 3-5 business days.</p>",
                bookingReference, reason
        );

        String smsBody = String.format(
                "HMS Alert: Your booking %s has been cancelled. Reason: %s.",
                bookingReference, reason
        );

        dispatchAndLog(customerId, recipientEmail, NotificationChannel.EMAIL, NotificationType.BOOKING_CANCELLED,
                bookingReference, emailSubject, emailBody);
        dispatchAndLog(customerId, recipientPhone, NotificationChannel.SMS, NotificationType.BOOKING_CANCELLED,
                bookingReference, "Booking Cancelled", smsBody);
    }

    @Override
    @Transactional
    public void handlePaymentProcessed(String invoiceNumber, String bookingReference, Long customerId,
                                       BigDecimal totalAmount, String status) {
        log.info("Handling payment processed notification for invoice: {}", invoiceNumber);
        String recipientEmail = "customer" + customerId + "@hms-guest.com";

        String emailSubject = "Payment Receipt & Tax Invoice - " + invoiceNumber;
        String emailBody = String.format(
                "<h2>Hospitality Management System - Payment Receipt</h2>" +
                "<p>Invoice Number: <b>%s</b></p>" +
                "<p>Booking Reference: <b>%s</b></p>" +
                "<p>Amount: <b>₹%s</b> (Inclusive of 18%% GST)</p>" +
                "<p>Payment Status: <b style='color:green;'>%s</b></p>",
                invoiceNumber, bookingReference, totalAmount, status
        );

        dispatchAndLog(customerId, recipientEmail, NotificationChannel.EMAIL, NotificationType.PAYMENT_SUCCESS,
                invoiceNumber, emailSubject, emailBody);
    }

    @Override
    @Transactional
    public void handleRefundProcessed(String invoiceNumber, String bookingReference, Long customerId,
                                      BigDecimal refundAmount, String reason) {
        log.info("Handling refund notification for invoice: {}", invoiceNumber);
        String recipientEmail = "customer" + customerId + "@hms-guest.com";
        String recipientPhone = "+91-9876543210";

        String emailSubject = "Refund Confirmation - " + invoiceNumber;
        String emailBody = String.format(
                "<h2>Refund Processed Successfully</h2>" +
                "<p>Invoice Number: <b>%s</b> (Booking: %s)</p>" +
                "<p>Refund Amount: <b>₹%s</b></p>" +
                "<p>Reason: <i>%s</i></p>",
                invoiceNumber, bookingReference, refundAmount, reason
        );

        String smsBody = String.format(
                "HMS: Refund of Rs. %s for invoice %s has been processed successfully.",
                refundAmount, invoiceNumber
        );

        dispatchAndLog(customerId, recipientEmail, NotificationChannel.EMAIL, NotificationType.REFUND_ISSUED,
                invoiceNumber, emailSubject, emailBody);
        dispatchAndLog(customerId, recipientPhone, NotificationChannel.SMS, NotificationType.REFUND_ISSUED,
                invoiceNumber, "Refund Processed", smsBody);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomerId(Long customerId) {
        return logRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void handleBookingExpired(String bookingReference, Long customerId, String reason) {
        log.info("Processing booking hold expired event for reference: {}", bookingReference);
        String subject = "Reservation Hold Expired — " + bookingReference;
        String content = String.format(
                "Dear Guest, your temporary hold for reservation %s has expired due to non-payment within 15 minutes. Reason: %s.",
                bookingReference, reason != null ? reason : "Hold timeout elapsed"
        );
        dispatchAndLog(customerId, "guest" + customerId + "@grandluxe.com", NotificationChannel.EMAIL,
                NotificationType.BOOKING_EXPIRED, bookingReference, subject, content);
    }

    @Override
    @Transactional
    public void handleRoomServiceRequested(Long requestId, String bookingReference, Long customerId, String serviceType) {
        log.info("Processing room service requested event for request ID: {}", requestId);
        String subject = "Service Order Received — " + serviceType;
        String content = String.format(
                "Your request for %s (Ref #%d) for booking %s has been received by our guest operations team.",
                serviceType, requestId, bookingReference
        );
        dispatchAndLog(customerId, "guest" + customerId + "@grandluxe.com", NotificationChannel.EMAIL,
                NotificationType.ROOM_SERVICE_REQUESTED, String.valueOf(requestId), subject, content);
    }

    @Override
    @Transactional
    public void handleInventoryLowStock(String itemCode, String itemName, int currentStock, int threshold) {
        log.warn("Processing low stock alert event for item: {} (Qty: {})", itemCode, currentStock);
        String subject = "STAFF ALERT: Low Inventory Warning for " + itemName;
        String content = String.format(
                "CRITICAL: Stock for item '%s' (Code: %s) has dropped to %d units, below threshold (%d units). Please restock immediately.",
                itemName, itemCode, currentStock, threshold
        );
        dispatchAndLog(0L, "operations@grandluxe.com", NotificationChannel.EMAIL,
                NotificationType.LOW_STOCK_ALERT, itemCode, subject, content);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long id) {
        return logRepository.findById(id)
                .map(this::mapToResponse)
                .orElse(null);
    }


    private void dispatchAndLog(Long customerId, String recipient, NotificationChannel channel,
                                NotificationType type, String refId, String subject, String content) {
        boolean success;
        if (channel == NotificationChannel.EMAIL) {
            success = emailDispatcher.sendEmail(recipient, subject, content);
        } else {
            success = smsDispatcher.sendSms(recipient, content);
        }

        NotificationLog nLog = NotificationLog.builder()
                .customerId(customerId)
                .recipient(recipient)
                .channel(channel)
                .type(type)
                .referenceId(refId)
                .subject(subject)
                .content(content)
                .status(success ? NotificationStatus.SENT : NotificationStatus.FAILED)
                .errorMessage(success ? null : "Simulated dispatcher delivery failure")
                .sentAt(success ? LocalDateTime.now() : null)
                .build();

        logRepository.save(nLog);
    }

    private NotificationResponse mapToResponse(NotificationLog log) {
        return NotificationResponse.builder()
                .id(log.getId())
                .customerId(log.getCustomerId())
                .recipient(log.getRecipient())
                .channel(log.getChannel())
                .type(log.getType())
                .referenceId(log.getReferenceId())
                .subject(log.getSubject())
                .content(log.getContent())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .sentAt(log.getSentAt())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
