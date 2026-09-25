package com.hospitality.notification.config;

import com.hospitality.notification.entity.NotificationChannel;
import com.hospitality.notification.entity.NotificationLog;
import com.hospitality.notification.entity.NotificationStatus;
import com.hospitality.notification.entity.NotificationType;
import com.hospitality.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final NotificationLogRepository logRepository;

    @Override
    public void run(String... args) {
        if (logRepository.count() > 0) {
            log.info("Notification database already seeded. Skipping initialization.");
            return;
        }

        log.info("Seeding initial welcome notification log into hms_notification_db...");

        NotificationLog welcomeLog = NotificationLog.builder()
                .customerId(1L)
                .recipient("john.doe@example.com")
                .channel(NotificationChannel.EMAIL)
                .type(NotificationType.DIRECT_ALERT)
                .referenceId("SYS-WELCOME-001")
                .subject("Welcome to Hospitality Management System!")
                .content("Dear John, welcome to our luxury hotel booking network. Explore our properties and enjoy exclusive stay vouchers.")
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now().minusDays(2))
                .build();

        logRepository.save(welcomeLog);
        log.info("Successfully seeded demo notification into hms_notification_db.");
    }
}
