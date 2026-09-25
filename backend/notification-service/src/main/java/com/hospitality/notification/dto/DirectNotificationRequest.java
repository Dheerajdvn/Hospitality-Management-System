package com.hospitality.notification.dto;

import com.hospitality.notification.entity.NotificationChannel;
import com.hospitality.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectNotificationRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "Recipient address or phone number is required")
    private String recipient;

    @NotNull(message = "Notification channel is required")
    private NotificationChannel channel;

    @Builder.Default
    private NotificationType type = NotificationType.DIRECT_ALERT;

    private String referenceId;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;
}
