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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodServiceImpl implements FoodService {

    private final FoodItemRepository foodItemRepository;
    private final HotelClient hotelClient;

    @Override
    @Transactional
    public FoodItemResponse createFoodItem(FoodItemRequest request) {
        log.info("Creating food item '{}' for hotel ID {}", request.getName(), request.getHotelId());

        // Validate hotel exists and is active
        validateHotelExistsAndActive(request.getHotelId());

        // Check if duplicate item name exists in the same hotel
        if (foodItemRepository.existsByHotelIdAndNameIgnoreCase(request.getHotelId(), request.getName())) {
            throw new BadRequestException("Food item '" + request.getName() + "' already exists for hotel ID " + request.getHotelId());
        }

        FoodItem item = FoodItem.builder()
                .hotelId(request.getHotelId())
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .dietaryType(request.getDietaryType())
                .price(request.getPrice())
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .preparationTimeMinutes(request.getPreparationTimeMinutes() != null ? request.getPreparationTimeMinutes() : 20)
                .imageUrl(request.getImageUrl())
                .build();

        FoodItem saved = foodItemRepository.save(item);
        log.info("Successfully created food item ID {} for hotel ID {}", saved.getId(), saved.getHotelId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodItemResponse getFoodItemById(Long id) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with ID: " + id));
        return mapToResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItemResponse> getMenuByHotel(Long hotelId, FoodCategory category, DietaryType dietaryType, Boolean availableOnly) {
        log.debug("Fetching menu for hotel ID {} with category={}, dietaryType={}, availableOnly={}",
                hotelId, category, dietaryType, availableOnly);

        Specification<FoodItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("hotelId"), hotelId));

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (dietaryType != null) {
                predicates.add(cb.equal(root.get("dietaryType"), dietaryType));
            }
            if (availableOnly != null && availableOnly) {
                predicates.add(cb.isTrue(root.get("isAvailable")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return foodItemRepository.findAll(spec)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FoodItemResponse updateFoodItem(Long id, FoodItemRequest request) {
        log.info("Updating food item ID: {}", id);

        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with ID: " + id));

        // If hotelId changed, revalidate
        if (!item.getHotelId().equals(request.getHotelId())) {
            validateHotelExistsAndActive(request.getHotelId());
            item.setHotelId(request.getHotelId());
        }

        // If name changed, check uniqueness within hotel
        if (!item.getName().equalsIgnoreCase(request.getName().trim())) {
            if (foodItemRepository.existsByHotelIdAndNameIgnoreCase(request.getHotelId(), request.getName().trim())) {
                throw new BadRequestException("Food item '" + request.getName() + "' already exists for hotel ID " + request.getHotelId());
            }
            item.setName(request.getName().trim());
        }

        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        item.setDietaryType(request.getDietaryType());
        item.setPrice(request.getPrice());
        if (request.getIsAvailable() != null) {
            item.setIsAvailable(request.getIsAvailable());
        }
        if (request.getPreparationTimeMinutes() != null) {
            item.setPreparationTimeMinutes(request.getPreparationTimeMinutes());
        }
        item.setImageUrl(request.getImageUrl());

        FoodItem updated = foodItemRepository.save(item);
        log.info("Updated food item ID: {}", updated.getId());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public FoodItemResponse toggleAvailability(Long id, boolean isAvailable) {
        log.info("Toggling availability for food item ID {} to {}", id, isAvailable);

        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with ID: " + id));

        item.setIsAvailable(isAvailable);
        FoodItem updated = foodItemRepository.save(item);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteFoodItem(Long id) {
        log.info("Deleting food item ID: {}", id);

        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with ID: " + id));

        foodItemRepository.delete(item);
        log.info("Deleted food item ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItemResponse> getFoodItemsBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return foodItemRepository.findByIdIn(ids)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateHotelExistsAndActive(Long hotelId) {
        try {
            ApiResponse<Boolean> response = hotelClient.validateHotelActive(hotelId);
            if (response == null || !response.isSuccess() || !Boolean.TRUE.equals(response.getData())) {
                throw new BadRequestException("Hotel with ID " + hotelId + " does not exist or is inactive");
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to validate hotel ID {} via Feign: {}", hotelId, ex.getMessage());
            throw new BadRequestException("Unable to verify hotel with ID " + hotelId + ": " + ex.getMessage());
        }
    }

    private FoodItemResponse mapToResponse(FoodItem item) {
        return FoodItemResponse.builder()
                .id(item.getId())
                .hotelId(item.getHotelId())
                .name(item.getName())
                .description(item.getDescription())
                .category(item.getCategory())
                .dietaryType(item.getDietaryType())
                .price(item.getPrice())
                .isAvailable(item.getIsAvailable())
                .preparationTimeMinutes(item.getPreparationTimeMinutes())
                .imageUrl(item.getImageUrl())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
