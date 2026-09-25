package com.hospitality.food.dto;

import com.hospitality.food.entity.DietaryType;
import com.hospitality.food.entity.FoodCategory;
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
public class FoodItemResponse {

    private Long id;
    private Long hotelId;
    private String name;
    private String description;
    private FoodCategory category;
    private DietaryType dietaryType;
    private BigDecimal price;
    private Boolean isAvailable;
    private Integer preparationTimeMinutes;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
