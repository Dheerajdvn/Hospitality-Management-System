package com.hospitality.inventory.service;

import com.hospitality.inventory.dto.InventoryItemRequest;
import com.hospitality.inventory.dto.InventoryItemResponse;
import com.hospitality.inventory.dto.StockMovementRequest;
import com.hospitality.inventory.dto.StockMovementResponse;
import com.hospitality.inventory.entity.InventoryCategory;

import java.util.List;

public interface InventoryService {

    InventoryItemResponse createItem(InventoryItemRequest request);

    InventoryItemResponse getItemById(Long id);

    InventoryItemResponse getItemByCode(String itemCode);

    List<InventoryItemResponse> getItemsByHotel(Long hotelId, InventoryCategory category);

    List<InventoryItemResponse> getLowStockItems(Long hotelId);

    InventoryItemResponse updateStock(Long itemId, StockMovementRequest request);

    List<StockMovementResponse> getItemMovementLogs(Long itemId);

    InventoryItemResponse updateItem(Long id, InventoryItemRequest request);
}
