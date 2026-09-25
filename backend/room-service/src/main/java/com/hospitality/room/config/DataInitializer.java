package com.hospitality.room.config;

import com.hospitality.room.entity.Room;
import com.hospitality.room.entity.RoomStatus;
import com.hospitality.room.entity.RoomStatusLog;
import com.hospitality.room.entity.RoomType;
import com.hospitality.room.repository.RoomRepository;
import com.hospitality.room.repository.RoomStatusLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoomRepository roomRepository;
    private final RoomStatusLogRepository roomStatusLogRepository;

    @Override
    public void run(String... args) {
        if (roomRepository.count() > 0) {
            log.info("Room database already seeded. Skipping initialization.");
            return;
        }

        log.info("Seeding initial room inventory into hms_room_db...");

        List<Room> initialRooms = List.of(
                // Hotel 1: Grand Palace Hotel (Mumbai)
                Room.builder()
                        .hotelId(1L)
                        .roomNumber("101")
                        .type(RoomType.SINGLE)
                        .status(RoomStatus.AVAILABLE)
                        .basePrice(new BigDecimal("3500.00"))
                        .capacity(1)
                        .floorNumber(1)
                        .description("Cozy single room with high-speed WiFi and city view.")
                        .amenities(Set.of("WiFi", "Air Conditioning", "Smart TV", "Mini Fridge"))
                        .build(),
                Room.builder()
                        .hotelId(1L)
                        .roomNumber("102")
                        .type(RoomType.DOUBLE)
                        .status(RoomStatus.AVAILABLE)
                        .basePrice(new BigDecimal("5500.00"))
                        .capacity(2)
                        .floorNumber(1)
                        .description("Spacious double bedroom ideal for couples or business travelers.")
                        .amenities(Set.of("WiFi", "Air Conditioning", "Smart TV", "Mini Fridge", "Work Desk"))
                        .build(),
                Room.builder()
                        .hotelId(1L)
                        .roomNumber("201")
                        .type(RoomType.DELUXE)
                        .status(RoomStatus.AVAILABLE)
                        .basePrice(new BigDecimal("8500.00"))
                        .capacity(3)
                        .floorNumber(2)
                        .description("Premium deluxe room with king bed, balcony, and panoramic sea view.")
                        .amenities(Set.of("WiFi", "Air Conditioning", "Smart TV", "Mini Bar", "Sea View", "King Bed"))
                        .build(),
                Room.builder()
                        .hotelId(1L)
                        .roomNumber("301")
                        .type(RoomType.SUITE)
                        .status(RoomStatus.AVAILABLE)
                        .basePrice(new BigDecimal("15000.00"))
                        .capacity(4)
                        .floorNumber(3)
                        .description("Luxury presidential suite with separate living area, jacuzzi, and butler service.")
                        .amenities(Set.of("WiFi", "Air Conditioning", "Smart TV", "Jacuzzi", "Ocean View", "Balcony", "Butler Service"))
                        .build(),

                // Hotel 2: The Heritage Resort & Spa (Jaipur)
                Room.builder()
                        .hotelId(2L)
                        .roomNumber("101")
                        .type(RoomType.DOUBLE)
                        .status(RoomStatus.AVAILABLE)
                        .basePrice(new BigDecimal("6000.00"))
                        .capacity(2)
                        .floorNumber(1)
                        .description("Traditional Rajasthani architecture room facing landscaped gardens.")
                        .amenities(Set.of("WiFi", "Air Conditioning", "Garden View", "Breakfast Included", "Heritage Decor"))
                        .build(),
                Room.builder()
                        .hotelId(2L)
                        .roomNumber("102")
                        .type(RoomType.DELUXE)
                        .status(RoomStatus.AVAILABLE)
                        .basePrice(new BigDecimal("9500.00"))
                        .capacity(3)
                        .floorNumber(1)
                        .description("Deluxe heritage suite featuring handcrafted furniture and a marble bathtub.")
                        .amenities(Set.of("WiFi", "Air Conditioning", "Marble Bathtub", "Heritage Decor", "Balcony"))
                        .build(),
                Room.builder()
                        .hotelId(2L)
                        .roomNumber("201")
                        .type(RoomType.SUITE)
                        .status(RoomStatus.AVAILABLE)
                        .basePrice(new BigDecimal("18000.00"))
                        .capacity(4)
                        .floorNumber(2)
                        .description("Royal Rajput suite with private plunge pool and fort view.")
                        .amenities(Set.of("WiFi", "Air Conditioning", "Private Plunge Pool", "Fort View", "Butler Service"))
                        .build(),

                // Hotel 3: Royal Orchid Business Hotel (Bangalore)
                Room.builder()
                        .hotelId(3L)
                        .roomNumber("101")
                        .type(RoomType.SINGLE)
                        .status(RoomStatus.AVAILABLE)
                        .basePrice(new BigDecimal("4000.00"))
                        .capacity(1)
                        .floorNumber(1)
                        .description("Modern compact room optimized for corporate business executives.")
                        .amenities(Set.of("WiFi", "Air Conditioning", "Ergonomic Work Desk", "High Speed Internet"))
                        .build(),
                Room.builder()
                        .hotelId(3L)
                        .roomNumber("102")
                        .type(RoomType.DOUBLE)
                        .status(RoomStatus.AVAILABLE)
                        .basePrice(new BigDecimal("6500.00"))
                        .capacity(2)
                        .floorNumber(1)
                        .description("Contemporary executive double room near tech corridor.")
                        .amenities(Set.of("WiFi", "Air Conditioning", "Smart TV", "Work Desk", "Espresso Machine"))
                        .build(),
                Room.builder()
                        .hotelId(3L)
                        .roomNumber("201")
                        .type(RoomType.EXECUTIVE)
                        .status(RoomStatus.AVAILABLE)
                        .basePrice(new BigDecimal("11000.00"))
                        .capacity(2)
                        .floorNumber(2)
                        .description("Executive club floor room with complimentary executive lounge access.")
                        .amenities(Set.of("WiFi", "Air Conditioning", "Executive Lounge Access", "City View", "Espresso Machine"))
                        .build()
        );

        List<Room> savedRooms = roomRepository.saveAll(initialRooms);

        // Record initial status logs for audit trail
        savedRooms.forEach(room -> {
            RoomStatusLog statusLog = RoomStatusLog.builder()
                    .roomId(room.getId())
                    .previousStatus(null)
                    .newStatus(RoomStatus.AVAILABLE)
                    .changedBy("SYSTEM")
                    .reason("Initial seed data load")
                    .build();
            roomStatusLogRepository.save(statusLog);
        });

        log.info("Successfully seeded {} rooms and audit logs into hms_room_db.", savedRooms.size());
    }
}
