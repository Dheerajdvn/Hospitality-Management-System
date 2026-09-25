package com.hospitality.inventory.repository;

import com.hospitality.inventory.entity.StockMovementLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementLogRepository extends JpaRepository<StockMovementLog, Long> {

    List<StockMovementLog> findByItemIdOrderByCreatedAtDesc(Long itemId);
}
