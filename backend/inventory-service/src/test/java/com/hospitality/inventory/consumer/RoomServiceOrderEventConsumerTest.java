package com.hospitality.inventory.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitality.inventory.dto.StockMovementRequest;
import com.hospitality.inventory.entity.InventoryCategory;
import com.hospitality.inventory.entity.InventoryItem;
import com.hospitality.inventory.entity.StockMovementType;
import com.hospitality.inventory.entity.UnitOfMeasure;
import com.hospitality.inventory.repository.InventoryItemRepository;
import com.hospitality.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceOrderEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryItemRepository itemRepository;

    private RoomServiceOrderEventConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new RoomServiceOrderEventConsumer(inventoryService, itemRepository, objectMapper);
    }

    @Test
    @DisplayName("Should successfully consume room service order event and deduct matching inventory items")
    void consumeRoomServiceEvent_Success() {
        InventoryItem item = InventoryItem.builder()
                .id(10L)
                .hotelId(1L)
                .itemCode("INV-FOD-BUR-001")
                .name("Grand Truffle Burger")
                .category(InventoryCategory.KITCHEN_PANTRY)
                .unit(UnitOfMeasure.PIECES)
                .quantityAvailable(50)
                .reorderLevel(10)
                .unitCost(new BigDecimal("250.00"))
                .build();

        when(itemRepository.findAll()).thenReturn(List.of(item));

        String payload = """
                {
                  "orderId": 101,
                  "orderNumber": "RSM-2026-0001",
                  "bookingId": 501,
                  "hotelId": 1,
                  "items": [
                    {
                      "foodItemId": 1,
                      "foodItemName": "Grand Truffle Burger",
                      "quantity": 2,
                      "unitPrice": 450.00
                    }
                  ]
                }
                """;

        consumer.consumeRoomServiceEvent(payload);

        ArgumentCaptor<StockMovementRequest> captor = ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(inventoryService, times(1)).updateStock(eq(10L), captor.capture());

        StockMovementRequest captured = captor.getValue();
        assertEquals(StockMovementType.STOCK_OUT, captured.getMovementType());
        assertEquals(2, captured.getQuantity());
        assertEquals("In-room dining order: RSM-2026-0001", captured.getReason());
    }

    @Test
    @DisplayName("Should skip stock deduction when no matching inventory item is found")
    void consumeRoomServiceEvent_NoMatchingItem() {
        when(itemRepository.findAll()).thenReturn(List.of());

        String payload = """
                {
                  "orderId": 102,
                  "orderNumber": "RSM-2026-0002",
                  "bookingId": 502,
                  "hotelId": 1,
                  "items": [
                    {
                      "foodItemId": 99,
                      "foodItemName": "Custom Unlisted Cocktail",
                      "quantity": 1,
                      "unitPrice": 350.00
                    }
                  ]
                }
                """;

        consumer.consumeRoomServiceEvent(payload);

        verify(inventoryService, never()).updateStock(any(), any());
    }
}
