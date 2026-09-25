package com.hospitality.food.repository;

import com.hospitality.food.entity.DietaryType;
import com.hospitality.food.entity.FoodCategory;
import com.hospitality.food.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long>, JpaSpecificationExecutor<FoodItem> {

    List<FoodItem> findByHotelId(Long hotelId);

    List<FoodItem> findByHotelIdAndIsAvailableTrue(Long hotelId);

    List<FoodItem> findByHotelIdAndCategory(Long hotelId, FoodCategory category);

    List<FoodItem> findByHotelIdAndDietaryType(Long hotelId, DietaryType dietaryType);

    List<FoodItem> findByHotelIdAndCategoryAndDietaryType(Long hotelId, FoodCategory category, DietaryType dietaryType);

    List<FoodItem> findByIdIn(List<Long> ids);

    boolean existsByHotelIdAndNameIgnoreCase(Long hotelId, String name);
}
