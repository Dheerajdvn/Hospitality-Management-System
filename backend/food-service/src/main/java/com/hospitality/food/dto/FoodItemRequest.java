package com.hospitality.food.dto;

import com.hospitality.food.entity.DietaryType;
import com.hospitality.food.entity.FoodCategory;
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
public class FoodItemRequest {

    @NotNull(message = "Hotel ID is required")
    private Long hotelId;

    @NotBlank(message = "Food item name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Food category is required")
    private FoodCategory category;

    @NotNull(message = "Dietary type is required")
    private DietaryType dietaryType;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @Builder.Default
    private Boolean isAvailable = true;

    @Min(value = 1, message = "Preparation time must be at least 1 minute")
    @Max(value = 240, message = "Preparation time cannot exceed 240 minutes")
    @Builder.Default
    private Integer preparationTimeMinutes = 20;

    private String imageUrl;
}
