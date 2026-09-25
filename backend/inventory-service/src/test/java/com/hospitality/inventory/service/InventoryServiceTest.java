package com.hospitality.inventory.service;

import com.hospitality.inventory.client.HotelClient;
import com.hospitality.inventory.dto.*;
import com.hospitality.inventory.entity.*;
import com.hospitality.inventory.exception.BadRequestException;
import com.hospitality.inventory.exception.ResourceNotFoundException;
import com.hospitality.inventory.repository.InventoryItemRepository;
import com.hospitality.inventory.repository.StockMovementLogRepository;
import com.hospitality.inventory.publisher.InventoryEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private StockMovementLogRepository stockMovementLogRepository;

    @Mock
    private HotelClient hotelClient;

    @Mock
    private InventoryEventPublisher eventPublisher;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private InventoryItem sampleItem;
    private InventoryItemRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleItem = InventoryItem.builder()
                .id(1L)
                .hotelId(1L)
                .itemCode("INV-MUM-LIN-001")
                .name("King Bed Sheet")
                .description("Egyptian Cotton 300TC")
                .category(InventoryCategory.LINEN)
                .unit(UnitOfMeasure.PIECES)
                .quantityAvailable(100)
                .reorderLevel(20)
                .unitCost(new BigDecimal("850.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = InventoryItemRequest.builder()
                .hotelId(1L)
                .itemCode("INV-MUM-LIN-001")
                .name("King Bed Sheet")
                .description("Egyptian Cotton 300TC")
                .category(InventoryCategory.LINEN)
                .unit(UnitOfMeasure.PIECES)
                .quantityAvailable(100)
                .reorderLevel(20)
                .unitCost(new BigDecimal("850.00"))
                .build();
    }

    @Test
    @DisplayName("Should successfully create inventory item and record initial stock log")
    void createItem_Success() {
        when(hotelClient.validateHotelActive(1L)).thenReturn(ApiResponse.success("Hotel active", true));
        when(inventoryItemRepository.existsByHotelIdAndItemCode(1L, "INV-MUM-LIN-001")).thenReturn(false);
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(sampleItem);

        InventoryItemResponse response = inventoryService.createItem(sampleRequest);

        assertNotNull(response);
        assertEquals("INV-MUM-LIN-001", response.getItemCode());
        assertEquals(100, response.getQuantityAvailable());
        assertFalse(response.isLowStock());
        verify(inventoryItemRepository, times(1)).save(any(InventoryItem.class));
        verify(stockMovementLogRepository, times(1)).save(any(StockMovementLog.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when hotel is invalid or inactive")
    void createItem_InvalidHotel() {
        when(hotelClient.validateHotelActive(999L)).thenReturn(ApiResponse.error("Hotel not found"));
        sampleRequest.setHotelId(999L);

        assertThrows(BadRequestException.class, () -> inventoryService.createItem(sampleRequest));
        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when item code already exists for hotel")
    void createItem_DuplicateItemCode() {
        when(hotelClient.validateHotelActive(1L)).thenReturn(ApiResponse.success("Hotel active", true));
        when(inventoryItemRepository.existsByHotelIdAndItemCode(1L, "INV-MUM-LIN-001")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> inventoryService.createItem(sampleRequest));
        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    @DisplayName("Should successfully get item by ID")
    void getItemById_Success() {
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));

        InventoryItemResponse response = inventoryService.getItemById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("King Bed Sheet", response.getName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when item ID does not exist")
    void getItemById_NotFound() {
        when(inventoryItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> inventoryService.getItemById(999L));
    }

    @Test
    @DisplayName("Should successfully return low stock items")
    void getLowStockItems_Success() {
        sampleItem.setQuantityAvailable(15); // 15 <= 20
        when(inventoryItemRepository.findLowStockItems(1L)).thenReturn(List.of(sampleItem));

        List<InventoryItemResponse> lowStock = inventoryService.getLowStockItems(1L);

        assertNotNull(lowStock);
        assertEquals(1, lowStock.size());
        assertTrue(lowStock.get(0).isLowStock());
    }

    @Test
    @DisplayName("Should successfully process STOCK_IN and increment quantity")
    void updateStock_StockIn() {
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = StockMovementRequest.builder()
                .movementType(StockMovementType.STOCK_IN)
                .quantity(50)
                .reason("New shipment received")
                .performedBy("SUPERVISOR_ALOK")
                .build();

        InventoryItemResponse response = inventoryService.updateStock(1L, request);

        assertNotNull(response);
        assertEquals(150, response.getQuantityAvailable()); // 100 + 50
        verify(stockMovementLogRepository, times(1)).save(any(StockMovementLog.class));
    }

    @Test
    @DisplayName("Should successfully process STOCK_OUT and decrement quantity")
    void updateStock_StockOut_Success() {
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = StockMovementRequest.builder()
                .movementType(StockMovementType.STOCK_OUT)
                .quantity(30)
                .reason("Issued to Housekeeping Floor 2")
                .performedBy("HK_LEAD_SUNITA")
                .build();

        InventoryItemResponse response = inventoryService.updateStock(1L, request);

        assertNotNull(response);
        assertEquals(70, response.getQuantityAvailable()); // 100 - 30
        verify(stockMovementLogRepository, times(1)).save(any(StockMovementLog.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException on STOCK_OUT when requested exceeds available inventory")
    void updateStock_StockOut_InsufficientInventory() {
        sampleItem.setQuantityAvailable(10);
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));

        StockMovementRequest request = StockMovementRequest.builder()
                .movementType(StockMovementType.STOCK_OUT)
                .quantity(25) // 25 > 10
                .reason("Bulk request")
                .build();

        assertThrows(BadRequestException.class, () -> inventoryService.updateStock(1L, request));
        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    @DisplayName("Should successfully process ADJUSTMENT and set exact count")
    void updateStock_Adjustment() {
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = StockMovementRequest.builder()
                .movementType(StockMovementType.ADJUSTMENT)
                .quantity(95)
                .reason("Quarterly audit count correction")
                .performedBy("AUDITOR_RAJESH")
                .build();

        InventoryItemResponse response = inventoryService.updateStock(1L, request);

        assertNotNull(response);
        assertEquals(95, response.getQuantityAvailable());
        verify(stockMovementLogRepository, times(1)).save(any(StockMovementLog.class));
        verify(eventPublisher, times(1)).publishInventoryUpdated(any());
    }

    @Test
    @DisplayName("Should publish both InventoryUpdatedEvent and InventoryLowStockEvent when stock breaches reorder level")
    void updateStock_StockOut_TriggersLowStockEvent() {
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Initial: 100, ReorderLevel: 20. Decrement by 85 -> newQty: 15 <= 20
        StockMovementRequest request = StockMovementRequest.builder()
                .movementType(StockMovementType.STOCK_OUT)
                .quantity(85)
                .reason("Heavy linen replacement")
                .performedBy("HK_LEAD_SUNITA")
                .build();

        InventoryItemResponse response = inventoryService.updateStock(1L, request);

        assertNotNull(response);
        assertEquals(15, response.getQuantityAvailable());
        assertTrue(response.isLowStock());
        verify(eventPublisher, times(1)).publishInventoryUpdated(any());
        verify(eventPublisher, times(1)).publishInventoryLowStock(any());
    }
}

