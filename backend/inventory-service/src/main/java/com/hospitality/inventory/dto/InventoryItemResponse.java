package com.hospitality.inventory.dto;

import com.hospitality.inventory.entity.InventoryCategory;
import com.hospitality.inventory.entity.UnitOfMeasure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemResponse {

    private Long id;
    private Long hotelId;
    private String itemCode;
    private String name;
    private String description;
    private InventoryCategory category;
    private UnitOfMeasure unit;
    private Integer quantityAvailable;
    private Integer reorderLevel;
    private BigDecimal unitCost;
    private boolean isLowStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
