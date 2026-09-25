package com.hospitality.inventory.repository;

import com.hospitality.inventory.entity.InventoryCategory;
import com.hospitality.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByHotelId(Long hotelId);

    List<InventoryItem> findByHotelIdAndCategory(Long hotelId, InventoryCategory category);

    Optional<InventoryItem> findByItemCode(String itemCode);

    boolean existsByHotelIdAndItemCode(Long hotelId, String itemCode);

    @Query("SELECT i FROM InventoryItem i WHERE i.hotelId = :hotelId AND i.quantityAvailable <= i.reorderLevel")
    List<InventoryItem> findLowStockItems(@Param("hotelId") Long hotelId);
}
