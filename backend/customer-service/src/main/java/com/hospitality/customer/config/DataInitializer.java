package com.hospitality.customer.config;

import com.hospitality.customer.entity.Address;
import com.hospitality.customer.entity.CustomerProfile;
import com.hospitality.customer.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CustomerProfileRepository customerRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking default customer profiles in hms_customer_db...");

        // Matches customer account seeded in auth-service (userId: 3)
        if (!customerRepository.existsByUserId(3L)) {
            Address address = Address.builder()
                    .street("100 Luxury Boulevard, Apt 4B")
                    .city("New York")
                    .state("NY")
                    .postalCode("10001")
                    .country("USA")
                    .build();

            CustomerProfile profile = CustomerProfile.builder()
                    .userId(3L)
                    .firstName("John")
                    .lastName("Doe")
                    .email("customer@hospitality.com")
                    .phoneNumber("+1-555-0199")
                    .idProofType("PASSPORT")
                    .idProofNumber("P987654321")
                    .address(address)
                    .preferences("High floor, Quiet room, King-size bed")
                    .isActive(true)
                    .build();

            customerRepository.save(profile);
            log.info("Default Customer Profile created for userId: 3 (customer@hospitality.com)");
        }
    }
}
