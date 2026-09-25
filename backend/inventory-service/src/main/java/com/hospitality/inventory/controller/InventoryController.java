package com.hospitality.inventory.controller;

import com.hospitality.inventory.dto.*;
import com.hospitality.inventory.entity.InventoryCategory;
import com.hospitality.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryItemResponse>> createItem(
            @Valid @RequestBody InventoryItemRequest request) {
        InventoryItemResponse created = inventoryService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inventory item created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getItemById(@PathVariable Long id) {
        InventoryItemResponse item = inventoryService.getItemById(id);
        return ResponseEntity.ok(ApiResponse.success("Inventory item retrieved successfully", item));
    }

    @GetMapping("/code/{itemCode}")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getItemByCode(@PathVariable String itemCode) {
        InventoryItemResponse item = inventoryService.getItemByCode(itemCode);
        return ResponseEntity.ok(ApiResponse.success("Inventory item retrieved successfully", item));
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getItemsByHotel(
            @PathVariable Long hotelId,
            @RequestParam(required = false) InventoryCategory category) {
        List<InventoryItemResponse> items = inventoryService.getItemsByHotel(hotelId, category);
        return ResponseEntity.ok(ApiResponse.success("Inventory items retrieved successfully", items));
    }

    @GetMapping("/hotel/{hotelId}/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getLowStockItems(
            @PathVariable Long hotelId) {
        List<InventoryItemResponse> lowStockItems = inventoryService.getLowStockItems(hotelId);
        return ResponseEntity.ok(ApiResponse.success("Low stock alert items retrieved successfully", lowStockItems));
    }

    @PostMapping("/{id}/movement")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody StockMovementRequest request) {
        InventoryItemResponse updated = inventoryService.updateStock(id, request);
        return ResponseEntity.ok(ApiResponse.success("Stock movement recorded successfully", updated));
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> getItemMovementLogs(
            @PathVariable Long id) {
        List<StockMovementResponse> logs = inventoryService.getItemMovementLogs(id);
        return ResponseEntity.ok(ApiResponse.success("Stock movement audit logs retrieved successfully", logs));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody InventoryItemRequest request) {
        InventoryItemResponse updated = inventoryService.updateItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Inventory item updated successfully", updated));
    }
}
