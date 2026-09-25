package com.hospitality.food.service;

import com.hospitality.food.dto.FoodItemRequest;
import com.hospitality.food.dto.FoodItemResponse;
import com.hospitality.food.entity.DietaryType;
import com.hospitality.food.entity.FoodCategory;

import java.util.List;

public interface FoodService {

    FoodItemResponse createFoodItem(FoodItemRequest request);

    FoodItemResponse getFoodItemById(Long id);

    List<FoodItemResponse> getMenuByHotel(Long hotelId, FoodCategory category, DietaryType dietaryType, Boolean availableOnly);

    FoodItemResponse updateFoodItem(Long id, FoodItemRequest request);

    FoodItemResponse toggleAvailability(Long id, boolean isAvailable);

    void deleteFoodItem(Long id);

    List<FoodItemResponse> getFoodItemsBatch(List<Long> ids);
}
