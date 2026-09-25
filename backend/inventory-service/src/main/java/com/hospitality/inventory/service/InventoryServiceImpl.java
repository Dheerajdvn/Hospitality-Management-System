package com.hospitality.inventory.service;

import com.hospitality.inventory.client.HotelClient;
import com.hospitality.inventory.dto.*;
import com.hospitality.inventory.entity.InventoryCategory;
import com.hospitality.inventory.entity.InventoryItem;
import com.hospitality.inventory.entity.StockMovementLog;
import com.hospitality.inventory.entity.StockMovementType;
import com.hospitality.inventory.exception.BadRequestException;
import com.hospitality.inventory.exception.ResourceNotFoundException;
import com.hospitality.inventory.repository.InventoryItemRepository;
import com.hospitality.inventory.repository.StockMovementLogRepository;
import com.hospitality.inventory.event.InventoryLowStockEvent;
import com.hospitality.inventory.event.InventoryUpdatedEvent;
import com.hospitality.inventory.publisher.InventoryEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementLogRepository stockMovementLogRepository;
    private final HotelClient hotelClient;
    private final InventoryEventPublisher eventPublisher;

    @Override
    @Transactional
    public InventoryItemResponse createItem(InventoryItemRequest request) {
        log.info("Creating inventory item '{}' with code '{}' for hotel ID {}",
                request.getName(), request.getItemCode(), request.getHotelId());

        // Validate hotel existence
        validateHotelExistsAndActive(request.getHotelId());

        // Check if itemCode already exists
        if (inventoryItemRepository.existsByHotelIdAndItemCode(request.getHotelId(), request.getItemCode().trim())) {
            throw new BadRequestException("Inventory item with code '" + request.getItemCode() +
                    "' already exists for hotel ID " + request.getHotelId());
        }

        InventoryItem item = InventoryItem.builder()
                .hotelId(request.getHotelId())
                .itemCode(request.getItemCode().trim())
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .unit(request.getUnit())
                .quantityAvailable(request.getQuantityAvailable() != null ? request.getQuantityAvailable() : 0)
                .reorderLevel(request.getReorderLevel() != null ? request.getReorderLevel() : 10)
                .unitCost(request.getUnitCost())
                .build();

        InventoryItem saved = inventoryItemRepository.save(item);

        // Record initial stock creation log if quantity > 0
        if (saved.getQuantityAvailable() > 0) {
            StockMovementLog initialLog = StockMovementLog.builder()
                    .itemId(saved.getId())
                    .movementType(StockMovementType.STOCK_IN)
                    .quantity(saved.getQuantityAvailable())
                    .previousQuantity(0)
                    .newQuantity(saved.getQuantityAvailable())
                    .reason("Initial stock provisioning")
                    .performedBy("SYSTEM_INIT")
                    .build();
            stockMovementLogRepository.save(initialLog);
        }

        log.info("Created inventory item ID {} with code '{}'", saved.getId(), saved.getItemCode());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResponse getItemById(Long id) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with ID: " + id));
        return mapToResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResponse getItemByCode(String itemCode) {
        InventoryItem item = inventoryItemRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with code: " + itemCode));
        return mapToResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getItemsByHotel(Long hotelId, InventoryCategory category) {
        List<InventoryItem> items;
        if (category != null) {
            items = inventoryItemRepository.findByHotelIdAndCategory(hotelId, category);
        } else {
            items = inventoryItemRepository.findByHotelId(hotelId);
        }
        return items.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getLowStockItems(Long hotelId) {
        log.debug("Fetching low-stock alert items for hotel ID: {}", hotelId);
        return inventoryItemRepository.findLowStockItems(hotelId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryItemResponse updateStock(Long itemId, StockMovementRequest request) {
        log.info("Updating stock for item ID {}: {} {}", itemId, request.getMovementType(), request.getQuantity());

        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with ID: " + itemId));

        int prevQty = item.getQuantityAvailable();
        int newQty;

        switch (request.getMovementType()) {
            case STOCK_IN -> newQty = prevQty + request.getQuantity();
            case STOCK_OUT -> {
                if (prevQty < request.getQuantity()) {
                    throw new BadRequestException("Insufficient inventory for item '" + item.getName() +
                            "'. Available: " + prevQty + ", Requested: " + request.getQuantity());
                }
                newQty = prevQty - request.getQuantity();
            }
            case ADJUSTMENT -> newQty = request.getQuantity();
            default -> throw new BadRequestException("Unsupported stock movement type: " + request.getMovementType());
        }

        item.setQuantityAvailable(newQty);
        InventoryItem savedItem = inventoryItemRepository.save(item);

        StockMovementLog movementLog = StockMovementLog.builder()
                .itemId(savedItem.getId())
                .movementType(request.getMovementType())
                .quantity(request.getQuantity())
                .previousQuantity(prevQty)
                .newQuantity(newQty)
                .reason(request.getReason() != null ? request.getReason() : request.getMovementType().name())
                .performedBy(request.getPerformedBy() != null ? request.getPerformedBy() : "STAFF")
                .build();

        stockMovementLogRepository.save(movementLog);
        log.info("Stock updated for item ID {}: {} -> {}", itemId, prevQty, newQty);

        // Publish inventory updated event
        eventPublisher.publishInventoryUpdated(InventoryUpdatedEvent.builder()
                .itemId(savedItem.getId())
                .hotelId(savedItem.getHotelId())
                .itemCode(savedItem.getItemCode())
                .itemName(savedItem.getName())
                .previousQuantity(prevQty)
                .newQuantity(newQty)
                .movementType(request.getMovementType().name())
                .performedBy(movementLog.getPerformedBy())
                .timestamp(LocalDateTime.now())
                .build());

        // Publish low stock alert if below or at reorder level
        if (newQty <= savedItem.getReorderLevel()) {
            log.warn("Item ID {} ('{}') breached reorder level! Available: {}, Reorder Level: {}",
                    savedItem.getId(), savedItem.getName(), newQty, savedItem.getReorderLevel());
            eventPublisher.publishInventoryLowStock(InventoryLowStockEvent.builder()
                    .itemId(savedItem.getId())
                    .hotelId(savedItem.getHotelId())
                    .itemCode(savedItem.getItemCode())
                    .itemName(savedItem.getName())
                    .currentStock(newQty)
                    .reorderLevel(savedItem.getReorderLevel())
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        return mapToResponse(savedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovementResponse> getItemMovementLogs(Long itemId) {
        // Verify item exists
        if (!inventoryItemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Inventory item not found with ID: " + itemId);
        }
        return stockMovementLogRepository.findByItemIdOrderByCreatedAtDesc(itemId)
                .stream()
                .map(this::mapMovementToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryItemResponse updateItem(Long id, InventoryItemRequest request) {
        log.info("Updating inventory item details for ID: {}", id);

        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with ID: " + id));

        // Revalidate hotel if changed
        if (!item.getHotelId().equals(request.getHotelId())) {
            validateHotelExistsAndActive(request.getHotelId());
            item.setHotelId(request.getHotelId());
        }

        // Check unique code if changed
        if (!item.getItemCode().equalsIgnoreCase(request.getItemCode().trim())) {
            if (inventoryItemRepository.existsByHotelIdAndItemCode(request.getHotelId(), request.getItemCode().trim())) {
                throw new BadRequestException("Inventory item with code '" + request.getItemCode() +
                        "' already exists for hotel ID " + request.getHotelId());
            }
            item.setItemCode(request.getItemCode().trim());
        }

        item.setName(request.getName().trim());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        item.setUnit(request.getUnit());
        if (request.getReorderLevel() != null) {
            item.setReorderLevel(request.getReorderLevel());
        }
        item.setUnitCost(request.getUnitCost());

        InventoryItem saved = inventoryItemRepository.save(item);
        return mapToResponse(saved);
    }

    private void validateHotelExistsAndActive(Long hotelId) {
        try {
            ApiResponse<Boolean> response = hotelClient.validateHotelActive(hotelId);
            if (response == null || !response.isSuccess() || !Boolean.TRUE.equals(response.getData())) {
                throw new BadRequestException("Hotel with ID " + hotelId + " does not exist or is inactive");
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to validate hotel ID {} via Feign: {}", hotelId, ex.getMessage());
            throw new BadRequestException("Unable to verify hotel with ID " + hotelId + ": " + ex.getMessage());
        }
    }

    private InventoryItemResponse mapToResponse(InventoryItem item) {
        boolean isLowStock = item.getQuantityAvailable() <= item.getReorderLevel();
        return InventoryItemResponse.builder()
                .id(item.getId())
                .hotelId(item.getHotelId())
                .itemCode(item.getItemCode())
                .name(item.getName())
                .description(item.getDescription())
                .category(item.getCategory())
                .unit(item.getUnit())
                .quantityAvailable(item.getQuantityAvailable())
                .reorderLevel(item.getReorderLevel())
                .unitCost(item.getUnitCost())
                .isLowStock(isLowStock)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private StockMovementResponse mapMovementToResponse(StockMovementLog log) {
        return StockMovementResponse.builder()
                .id(log.getId())
                .itemId(log.getItemId())
                .movementType(log.getMovementType())
                .quantity(log.getQuantity())
                .previousQuantity(log.getPreviousQuantity())
                .newQuantity(log.getNewQuantity())
                .reason(log.getReason())
                .performedBy(log.getPerformedBy())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
