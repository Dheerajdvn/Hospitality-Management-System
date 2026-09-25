package com.hospitality.billing.config;

import com.hospitality.billing.entity.Bill;
import com.hospitality.billing.entity.BillStatus;
import com.hospitality.billing.entity.PaymentMethod;
import com.hospitality.billing.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BillRepository billRepository;

    @Override
    public void run(String... args) {
        if (billRepository.count() > 0) {
            log.info("Billing database already seeded. Skipping initialization.");
            return;
        }

        log.info("Seeding initial paid invoice for demo booking into hms_billing_db...");

        Bill demoBill = Bill.builder()
                .invoiceNumber("INV-2026-DEMO0001")
                .transactionReference("TXN-DEMO0001")
                .bookingId(1L)
                .bookingReference("HMS-BK-DEMO0001")
                .customerId(1L)
                .hotelId(1L)
                .baseAmount(new BigDecimal("16500.00"))
                .taxRatePercentage(new BigDecimal("18.00"))
                .taxAmount(new BigDecimal("2970.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("19470.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(BillStatus.PAID)
                .paymentGatewayResponse("SUCCESS - Authorization: AUTH-748921")
                .paidAt(LocalDateTime.now().minusDays(1))
                .build();

        billRepository.save(demoBill);
        log.info("Successfully seeded demo invoice INV-2026-DEMO0001 into hms_billing_db.");
    }
}
