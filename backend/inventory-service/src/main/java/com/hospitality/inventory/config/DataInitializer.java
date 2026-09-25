package com.hospitality.inventory.config;

import com.hospitality.inventory.entity.*;
import com.hospitality.inventory.repository.InventoryItemRepository;
import com.hospitality.inventory.repository.StockMovementLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementLogRepository stockMovementLogRepository;

    @Override
    public void run(String... args) {
        if (inventoryItemRepository.count() > 0) {
            log.info("Inventory catalog already contains {} items. Skipping initial seeding.", inventoryItemRepository.count());
            return;
        }

        log.info("Seeding initial hotel supplies and asset inventory across hotels...");

        List<InventoryItem> items = List.of(
                // ==================== HOTEL 1 (The Grand Palace, Mumbai) ====================
                InventoryItem.builder()
                        .hotelId(1L)
                        .itemCode("INV-MUM-LIN-001")
                        .name("Egyptian Cotton King Bed Sheet (300 TC)")
                        .description("Crisp white premium 300 thread count king size fitted bed sheets")
                        .category(InventoryCategory.LINEN)
                        .unit(UnitOfMeasure.PIECES)
                        .quantityAvailable(120)
                        .reorderLevel(30)
                        .unitCost(new BigDecimal("850.00"))
                        .build(),

                InventoryItem.builder()
                        .hotelId(1L)
                        .itemCode("INV-MUM-LIN-002")
                        .name("Plush Turkish Bath Towel (650 GSM)")
                        .description("Ultra-absorbent luxury white combed cotton bath towels")
                        .category(InventoryCategory.LINEN)
                        .unit(UnitOfMeasure.PIECES)
                        .quantityAvailable(200)
                        .reorderLevel(50)
                        .unitCost(new BigDecimal("450.00"))
                        .build(),

                InventoryItem.builder()
                        .hotelId(1L)
                        .itemCode("INV-MUM-TOI-001")
                        .name("Forest Essentials Shampoo & Conditioner 50ml")
                        .description("Eco-friendly Ayurvedic guest room mini shampoo bottles")
                        .category(InventoryCategory.TOILETRIES)
                        .unit(UnitOfMeasure.PIECES)
                        .quantityAvailable(500)
                        .reorderLevel(100)
                        .unitCost(new BigDecimal("45.00"))
                        .build(),

                // INTENTIONAL LOW-STOCK ITEM (40 <= 50) to test alerting
                InventoryItem.builder()
                        .hotelId(1L)
                        .itemCode("INV-MUM-TOI-002")
                        .name("Bamboo Dental Hygiene Kit")
                        .description("Biodegradable bamboo toothbrush with charcoal herbal toothpaste")
                        .category(InventoryCategory.TOILETRIES)
                        .unit(UnitOfMeasure.PACKETS)
                        .quantityAvailable(40)
                        .reorderLevel(50)
                        .unitCost(new BigDecimal("35.00"))
                        .build(),

                InventoryItem.builder()
                        .hotelId(1L)
                        .itemCode("INV-MUM-KIT-001")
                        .name("Aged Basmati Rice 25kg Bag")
                        .description("Long grain fragrant royal basmati rice for dining and banquets")
                        .category(InventoryCategory.KITCHEN_PANTRY)
                        .unit(UnitOfMeasure.PACKETS)
                        .quantityAvailable(15)
                        .reorderLevel(5)
                        .unitCost(new BigDecimal("2400.00"))
                        .build(),

                InventoryItem.builder()
                        .hotelId(1L)
                        .itemCode("INV-MUM-CLN-001")
                        .name("Hospital Grade Surface Disinfectant 5L")
                        .description("Multi-surface concentrated germicidal disinfectant cleaner")
                        .category(InventoryCategory.CLEANING_SUPPLIES)
                        .unit(UnitOfMeasure.LITERS)
                        .quantityAvailable(30)
                        .reorderLevel(10)
                        .unitCost(new BigDecimal("650.00"))
                        .build(),

                InventoryItem.builder()
                        .hotelId(1L)
                        .itemCode("INV-MUM-MNT-001")
                        .name("Warm White 9W LED Bulbs (B22)")
                        .description("Energy efficient warm white LED lamps for guest room bedside lighting")
                        .category(InventoryCategory.MAINTENANCE)
                        .unit(UnitOfMeasure.PIECES)
                        .quantityAvailable(80)
                        .reorderLevel(20)
                        .unitCost(new BigDecimal("120.00"))
                        .build(),

                // ==================== HOTEL 2 (Ocean View Resort, Goa) ====================
                InventoryItem.builder()
                        .hotelId(2L)
                        .itemCode("INV-GOA-LIN-001")
                        .name("Cabana Striped Pool Towels (500 GSM)")
                        .description("Vibrant blue and white striped quick-dry pool and beach towels")
                        .category(InventoryCategory.LINEN)
                        .unit(UnitOfMeasure.PIECES)
                        .quantityAvailable(150)
                        .reorderLevel(40)
                        .unitCost(new BigDecimal("550.00"))
                        .build(),

                InventoryItem.builder()
                        .hotelId(2L)
                        .itemCode("INV-GOA-KIT-001")
                        .name("Coorg Arabica Coffee Beans (1kg)")
                        .description("Freshly roasted medium roast single origin coffee beans")
                        .category(InventoryCategory.KITCHEN_PANTRY)
                        .unit(UnitOfMeasure.KILOGRAMS)
                        .quantityAvailable(25)
                        .reorderLevel(10)
                        .unitCost(new BigDecimal("900.00"))
                        .build(),

                // ==================== HOTEL 3 (Himalayan Heritage, Shimla) ====================
                InventoryItem.builder()
                        .hotelId(3L)
                        .itemCode("INV-SHI-LIN-001")
                        .name("Thermal Fleece Winter Blankets")
                        .description("Extra warm double-layered fleece blanket for mountain chill")
                        .category(InventoryCategory.LINEN)
                        .unit(UnitOfMeasure.PIECES)
                        .quantityAvailable(90)
                        .reorderLevel(25)
                        .unitCost(new BigDecimal("1200.00"))
                        .build()
        );

        List<InventoryItem> savedItems = inventoryItemRepository.saveAll(items);

        // Record initial stock logs
        for (InventoryItem saved : savedItems) {
            StockMovementLog logEntry = StockMovementLog.builder()
                    .itemId(saved.getId())
                    .movementType(StockMovementType.STOCK_IN)
                    .quantity(saved.getQuantityAvailable())
                    .previousQuantity(0)
                    .newQuantity(saved.getQuantityAvailable())
                    .reason("Initial hotel setup stock procurement")
                    .performedBy("SYSTEM_INIT")
                    .build();
            stockMovementLogRepository.save(logEntry);
        }

        log.info("Successfully seeded {} inventory supply items with audit logs across 3 hotels.", savedItems.size());
    }
}
