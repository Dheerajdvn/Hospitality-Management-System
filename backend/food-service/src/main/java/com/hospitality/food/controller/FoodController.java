package com.hospitality.food.controller;

import com.hospitality.food.dto.ApiResponse;
import com.hospitality.food.dto.BatchFoodLookupRequest;
import com.hospitality.food.dto.FoodItemRequest;
import com.hospitality.food.dto.FoodItemResponse;
import com.hospitality.food.entity.DietaryType;
import com.hospitality.food.entity.FoodCategory;
import com.hospitality.food.service.FoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/food")
@RequiredArgsConstructor
@Slf4j
public class FoodController {

    private final FoodService foodService;

    @PostMapping
    public ResponseEntity<ApiResponse<FoodItemResponse>> createFoodItem(@Valid @RequestBody FoodItemRequest request) {
        FoodItemResponse created = foodService.createFoodItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Food item created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodItemResponse>> getFoodItemById(@PathVariable Long id) {
        FoodItemResponse item = foodService.getFoodItemById(id);
        return ResponseEntity.ok(ApiResponse.success("Food item retrieved successfully", item));
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> getMenuByHotel(
            @PathVariable Long hotelId,
            @RequestParam(required = false) FoodCategory category,
            @RequestParam(required = false) DietaryType dietaryType,
            @RequestParam(required = false) Boolean availableOnly) {

        List<FoodItemResponse> menu = foodService.getMenuByHotel(hotelId, category, dietaryType, availableOnly);
        return ResponseEntity.ok(ApiResponse.success("Menu retrieved successfully", menu));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodItemResponse>> updateFoodItem(
            @PathVariable Long id,
            @Valid @RequestBody FoodItemRequest request) {

        FoodItemResponse updated = foodService.updateFoodItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Food item updated successfully", updated));
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<FoodItemResponse>> patchAvailability(
            @PathVariable Long id,
            @RequestParam boolean available) {

        FoodItemResponse updated = foodService.toggleAvailability(id, available);
        return ResponseEntity.ok(ApiResponse.success("Food item availability updated to " + available, updated));
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<FoodItemResponse>> putAvailability(
            @PathVariable Long id,
            @RequestParam boolean available) {

        FoodItemResponse updated = foodService.toggleAvailability(id, available);
        return ResponseEntity.ok(ApiResponse.success("Food item availability updated to " + available, updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFoodItem(@PathVariable Long id) {
        foodService.deleteFoodItem(id);
        return ResponseEntity.ok(ApiResponse.success("Food item deleted successfully", null));
    }

    @PostMapping({"/batch", "/batch-lookup"})
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> getFoodItemsBatch(
            @Valid @RequestBody BatchFoodLookupRequest request) {

        List<FoodItemResponse> items = foodService.getFoodItemsBatch(request.getFoodItemIds());
        return ResponseEntity.ok(ApiResponse.success("Batch food items retrieved successfully", items));
    }

}
