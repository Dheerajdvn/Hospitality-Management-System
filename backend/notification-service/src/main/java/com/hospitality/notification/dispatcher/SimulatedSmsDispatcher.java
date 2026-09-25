package com.hospitality.notification.dispatcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimulatedSmsDispatcher {

    public boolean sendSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank() || phoneNumber.contains("0000000000")) {
            log.warn("[SIMULATED SMS FAILURE] Invalid phone number: {}", phoneNumber);
            return false;
        }

        log.info("--------------------------------------------------------------------------------");
        log.info("[SIMULATED SMS DISPATCH]");
        log.info("To: {}", phoneNumber);
        log.info("Message: {}", message);
        log.info("Status: DELIVERED (Simulated SMS Gateway)");
        log.info("--------------------------------------------------------------------------------");
        return true;
    }
}
