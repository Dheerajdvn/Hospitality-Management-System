package com.hospitality.inventory.dto;

import com.hospitality.inventory.entity.InventoryCategory;
import com.hospitality.inventory.entity.UnitOfMeasure;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemRequest {

    @NotNull(message = "Hotel ID is required")
    private Long hotelId;

    @NotBlank(message = "Item code is required")
    @Size(max = 50, message = "Item code must not exceed 50 characters")
    private String itemCode;

    @NotBlank(message = "Item name is required")
    @Size(max = 150, message = "Item name must not exceed 150 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Inventory category is required")
    private InventoryCategory category;

    @NotNull(message = "Unit of measure is required")
    private UnitOfMeasure unit;

    @Min(value = 0, message = "Quantity available cannot be negative")
    @Builder.Default
    private Integer quantityAvailable = 0;

    @Min(value = 0, message = "Reorder level cannot be negative")
    @Builder.Default
    private Integer reorderLevel = 10;

    @NotNull(message = "Unit cost is required")
    @DecimalMin(value = "0.01", message = "Unit cost must be greater than 0")
    private BigDecimal unitCost;
}
