package com.hospitality.notification.repository;

import com.hospitality.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<NotificationLog> findByReferenceIdOrderByCreatedAtDesc(String referenceId);

    List<NotificationLog> findByRecipientOrderByCreatedAtDesc(String recipient);
}
