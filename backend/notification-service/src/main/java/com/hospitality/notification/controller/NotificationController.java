package com.hospitality.notification.controller;

import com.hospitality.notification.dto.ApiResponse;
import com.hospitality.notification.dto.DirectNotificationRequest;
import com.hospitality.notification.dto.NotificationResponse;
import com.hospitality.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send-direct")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendDirectNotification(
            @Valid @RequestBody DirectNotificationRequest request) {
        NotificationResponse response = notificationService.sendDirectNotification(request);
        return new ResponseEntity<>(
                ApiResponse.success("Notification dispatched successfully", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByCustomerId(
            @PathVariable("customerId") Long customerId) {
        List<NotificationResponse> list = notificationService.getNotificationsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer notification history retrieved", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(@PathVariable("id") Long id) {
        NotificationResponse response = notificationService.getNotificationById(id);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Notification not found with ID: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("Notification details retrieved", response));
    }

    /**
     * Testing endpoint to simulate an asynchronous event trigger without a live Kafka broker.
     */
    @PostMapping("/simulate-event")
    public ResponseEntity<ApiResponse<String>> simulateEvent(
            @RequestParam("eventType") String eventType,
            @RequestParam("reference") String reference,
            @RequestParam("customerId") Long customerId,
            @RequestParam(value = "amount", defaultValue = "15000.00") BigDecimal amount) {
        switch (eventType.toUpperCase()) {
            case "BOOKING_CREATED" -> notificationService.handleBookingCreated(reference, customerId, amount, "15 minutes from now");
            case "BOOKING_CONFIRMED" -> notificationService.handleBookingConfirmed(reference, customerId, amount);
            case "BOOKING_CANCELLED" -> notificationService.handleBookingCancelled(reference, customerId, "User requested cancellation");
            case "PAYMENT_SUCCESS" -> notificationService.handlePaymentProcessed("INV-SIM-" + reference, reference, customerId, amount, "SUCCESS");
            case "REFUND_ISSUED" -> notificationService.handleRefundProcessed("INV-SIM-" + reference, reference, customerId, amount, "Cancelled booking refund");
            default -> {
                return ResponseEntity.badRequest().body(ApiResponse.error("Unknown eventType: " + eventType));
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Event '" + eventType + "' simulated and notifications dispatched."));
    }
}
