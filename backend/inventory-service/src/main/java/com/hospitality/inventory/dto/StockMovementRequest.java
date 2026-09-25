package com.hospitality.inventory.dto;

import com.hospitality.inventory.entity.StockMovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementRequest {

    @NotNull(message = "Movement type is required (STOCK_IN, STOCK_OUT, ADJUSTMENT)")
    private StockMovementType movementType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    @Size(max = 100, message = "Performed by cannot exceed 100 characters")
    private String performedBy;
}
