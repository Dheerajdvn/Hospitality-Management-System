package com.hospitality.hotel.config;

import com.hospitality.hotel.entity.Hotel;
import com.hospitality.hotel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final HotelRepository hotelRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (hotelRepository.count() == 0) {
            log.info("Bootstrapping sample hotel properties into hms_hotel_db...");

            Hotel hotel1 = Hotel.builder()
                    .name("The Grand Manhattan Hotel")
                    .description("Iconic 5-star luxury hotel in the heart of Midtown Manhattan with skyline views.")
                    .city("New York")
                    .state("NY")
                    .country("USA")
                    .address("768 5th Ave, New York, NY 10019")
                    .postalCode("10019")
                    .starRating(4.8)
                    .phoneNumber("+1-212-555-0101")
                    .email("concierge@grandmanhattan.com")
                    .imageUrl("https://images.unsplash.com/photo-1566073771259-6a8506099945")
                    .amenities(Set.of("HIGH_SPEED_WIFI", "SWIMMING_POOL", "LUXURY_SPA", "FITNESS_CENTER", "FINE_DINING", "VALET_PARKING"))
                    .isActive(true)
                    .build();

            Hotel hotel2 = Hotel.builder()
                    .name("Miami Oceanfront Luxury Resort")
                    .description("Tropical beachfront sanctuary featuring private beach cabanas and signature dining.")
                    .city("Miami")
                    .state("FL")
                    .country("USA")
                    .address("4441 Collins Ave, Miami Beach, FL 33140")
                    .postalCode("33140")
                    .starRating(4.9)
                    .phoneNumber("+1-305-555-0102")
                    .email("stay@miamioceanresort.com")
                    .imageUrl("https://images.unsplash.com/photo-1520250497591-112f2f40a3f4")
                    .amenities(Set.of("BEACHFRONT_ACCESS", "INFINITY_POOL", "WELLNESS_SPA", "COCKTAIL_LOUNGE", "24H_ROOM_SERVICE"))
                    .isActive(true)
                    .build();

            Hotel hotel3 = Hotel.builder()
                    .name("Alpine Peak Mountain Lodge")
                    .description("Charming mountain retreat nestled in the Rockies with ski-in/ski-out access.")
                    .city("Denver")
                    .state("CO")
                    .country("USA")
                    .address("1200 Mountain Ridge Rd, Denver, CO 80202")
                    .postalCode("80202")
                    .starRating(4.5)
                    .phoneNumber("+1-303-555-0103")
                    .email("info@alpinepeaklodge.com")
                    .imageUrl("https://images.unsplash.com/photo-1542314831-068cd1dbfeeb")
                    .amenities(Set.of("SKI_IN_SKI_OUT", "HEATED_POOL", "MOUNTAIN_VIEW", "FIREPLACE_LOUNGE", "SPA"))
                    .isActive(true)
                    .build();

            hotelRepository.saveAll(Set.of(hotel1, hotel2, hotel3));
            log.info("Successfully bootstrapped 3 sample hotel properties.");
        }
    }
}
