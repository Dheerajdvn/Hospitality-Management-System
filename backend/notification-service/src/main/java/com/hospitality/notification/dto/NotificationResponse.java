package com.hospitality.notification.dto;

import com.hospitality.notification.entity.NotificationChannel;
import com.hospitality.notification.entity.NotificationStatus;
import com.hospitality.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long customerId;
    private String recipient;
    private NotificationChannel channel;
    private NotificationType type;
    private String referenceId;
    private String subject;
    private String content;
    private NotificationStatus status;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
