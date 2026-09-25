package com.hospitality.rsm.repository;

import com.hospitality.rsm.entity.RoomServiceOrder;
import com.hospitality.rsm.entity.RoomServiceOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomServiceOrderRepository extends JpaRepository<RoomServiceOrder, Long> {

    Optional<RoomServiceOrder> findByOrderNumber(String orderNumber);

    List<RoomServiceOrder> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    List<RoomServiceOrder> findByHotelIdOrderByCreatedAtDesc(Long hotelId);

    List<RoomServiceOrder> findByHotelIdAndStatusOrderByCreatedAtDesc(Long hotelId, RoomServiceOrderStatus status);

    List<RoomServiceOrder> findByRoomIdOrderByCreatedAtDesc(Long roomId);

    boolean existsByOrderNumber(String orderNumber);
}
