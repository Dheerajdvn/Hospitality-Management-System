package com.hospitality.rsm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodItemDto {

    private Long id;
    private Long hotelId;
    private String name;
    private BigDecimal price;
    private Boolean isAvailable;
    private Integer preparationTimeMinutes;
}
