package com.hospitality.inventory.publisher;

import com.hospitality.inventory.event.InventoryLowStockEvent;
import com.hospitality.inventory.event.InventoryUpdatedEvent;

public interface InventoryEventPublisher {

    void publishInventoryUpdated(InventoryUpdatedEvent event);

    void publishInventoryLowStock(InventoryLowStockEvent event);
}
