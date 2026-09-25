package com.hospitality.food.service;

import com.hospitality.food.client.HotelClient;
import com.hospitality.food.dto.ApiResponse;
import com.hospitality.food.dto.FoodItemRequest;
import com.hospitality.food.dto.FoodItemResponse;
import com.hospitality.food.entity.DietaryType;
import com.hospitality.food.entity.FoodCategory;
import com.hospitality.food.entity.FoodItem;
import com.hospitality.food.exception.BadRequestException;
import com.hospitality.food.exception.ResourceNotFoundException;
import com.hospitality.food.repository.FoodItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @Mock
    private FoodItemRepository foodItemRepository;

    @Mock
    private HotelClient hotelClient;

    @InjectMocks
    private FoodServiceImpl foodService;

    private FoodItem sampleItem;
    private FoodItemRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleItem = FoodItem.builder()
                .id(1L)
                .hotelId(1L)
                .name("Paneer Tikka Royale")
                .description("Charred cottage cheese cubes")
                .category(FoodCategory.APPETIZER)
                .dietaryType(DietaryType.VEG)
                .price(new BigDecimal("350.00"))
                .isAvailable(true)
                .preparationTimeMinutes(20)
                .imageUrl("https://example.com/paneer.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = FoodItemRequest.builder()
                .hotelId(1L)
                .name("Paneer Tikka Royale")
                .description("Charred cottage cheese cubes")
                .category(FoodCategory.APPETIZER)
                .dietaryType(DietaryType.VEG)
                .price(new BigDecimal("350.00"))
                .isAvailable(true)
                .preparationTimeMinutes(20)
                .imageUrl("https://example.com/paneer.jpg")
                .build();
    }

    @Test
    @DisplayName("Should successfully create a food item when hotel is active and name is unique")
    void createFoodItem_Success() {
        when(hotelClient.validateHotelActive(1L)).thenReturn(ApiResponse.success("Hotel valid", true));
        when(foodItemRepository.existsByHotelIdAndNameIgnoreCase(1L, "Paneer Tikka Royale")).thenReturn(false);
        when(foodItemRepository.save(any(FoodItem.class))).thenReturn(sampleItem);

        FoodItemResponse response = foodService.createFoodItem(sampleRequest);

        assertNotNull(response);
        assertEquals("Paneer Tikka Royale", response.getName());
        assertEquals(new BigDecimal("350.00"), response.getPrice());
        assertEquals(DietaryType.VEG, response.getDietaryType());
        verify(foodItemRepository, times(1)).save(any(FoodItem.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when hotel is invalid or inactive")
    void createFoodItem_InvalidHotel() {
        when(hotelClient.validateHotelActive(999L)).thenReturn(ApiResponse.error("Hotel not found"));
        sampleRequest.setHotelId(999L);

        assertThrows(BadRequestException.class, () -> foodService.createFoodItem(sampleRequest));
        verify(foodItemRepository, never()).save(any(FoodItem.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when food item name already exists in same hotel")
    void createFoodItem_DuplicateName() {
        when(hotelClient.validateHotelActive(1L)).thenReturn(ApiResponse.success("Hotel valid", true));
        when(foodItemRepository.existsByHotelIdAndNameIgnoreCase(1L, "Paneer Tikka Royale")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> foodService.createFoodItem(sampleRequest));
        verify(foodItemRepository, never()).save(any(FoodItem.class));
    }

    @Test
    @DisplayName("Should successfully get food item by ID")
    void getFoodItemById_Success() {
        when(foodItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));

        FoodItemResponse response = foodService.getFoodItemById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Paneer Tikka Royale", response.getName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when food item ID does not exist")
    void getFoodItemById_NotFound() {
        when(foodItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> foodService.getFoodItemById(999L));
    }

    @Test
    @DisplayName("Should return menu filtered by hotel and criteria")
    void getMenuByHotel_Success() {
        when(foodItemRepository.findAll(any(Specification.class))).thenReturn(List.of(sampleItem));

        List<FoodItemResponse> menu = foodService.getMenuByHotel(1L, FoodCategory.APPETIZER, DietaryType.VEG, true);

        assertNotNull(menu);
        assertEquals(1, menu.size());
        assertEquals("Paneer Tikka Royale", menu.get(0).getName());
    }

    @Test
    @DisplayName("Should successfully toggle food item availability")
    void toggleAvailability_Success() {
        when(foodItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(foodItemRepository.save(any(FoodItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FoodItemResponse response = foodService.toggleAvailability(1L, false);

        assertNotNull(response);
        assertFalse(response.getIsAvailable());
        verify(foodItemRepository, times(1)).save(sampleItem);
    }

    @Test
    @DisplayName("Should successfully update food item details")
    void updateFoodItem_Success() {
        when(foodItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(foodItemRepository.save(any(FoodItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FoodItemRequest updateReq = FoodItemRequest.builder()
                .hotelId(1L)
                .name("Paneer Tikka Royale")
                .description("Updated description")
                .category(FoodCategory.APPETIZER)
                .dietaryType(DietaryType.VEG)
                .price(new BigDecimal("399.00"))
                .isAvailable(true)
                .preparationTimeMinutes(18)
                .imageUrl("https://example.com/paneer-new.jpg")
                .build();

        FoodItemResponse response = foodService.updateFoodItem(1L, updateReq);

        assertNotNull(response);
        assertEquals(new BigDecimal("399.00"), response.getPrice());
        assertEquals("Updated description", response.getDescription());
    }

    @Test
    @DisplayName("Should successfully return batch food items by IDs")
    void getFoodItemsBatch_Success() {
        when(foodItemRepository.findByIdIn(List.of(1L, 2L))).thenReturn(List.of(sampleItem));

        List<FoodItemResponse> items = foodService.getFoodItemsBatch(List.of(1L, 2L));

        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals(1L, items.get(0).getId());
    }
}
