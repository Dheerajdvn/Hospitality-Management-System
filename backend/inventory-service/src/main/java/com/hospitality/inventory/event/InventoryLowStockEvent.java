package com.hospitality.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLowStockEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;
    private Long hotelId;
    private String itemCode;
    private String itemName;
    private Integer currentStock;
    private Integer reorderLevel;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
