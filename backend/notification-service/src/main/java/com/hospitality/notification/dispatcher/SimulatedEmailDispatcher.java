package com.hospitality.notification.dispatcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimulatedEmailDispatcher {

    public boolean sendEmail(String toEmail, String subject, String bodyHtml) {
        if (toEmail == null || toEmail.isBlank() || toEmail.contains("invalid")) {
            log.warn("[SIMULATED EMAIL FAILURE] Invalid recipient address: {}", toEmail);
            return false;
        }

        log.info("================================================================================");
        log.info("[SIMULATED EMAIL DISPATCH]");
        log.info("To: {}", toEmail);
        log.info("Subject: {}", subject);
        log.info("Body (HTML preview): {}", bodyHtml.length() > 200 ? bodyHtml.substring(0, 200) + "..." : bodyHtml);
        log.info("Status: DELIVERED (Simulated SMTP)");
        log.info("================================================================================");
        return true;
    }
}
