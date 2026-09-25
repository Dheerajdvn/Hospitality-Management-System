package com.hospitality.room.repository;

import com.hospitality.room.entity.Room;
import com.hospitality.room.entity.RoomStatus;
import com.hospitality.room.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelId(Long hotelId);

    List<Room> findByHotelIdAndStatus(Long hotelId, RoomStatus status);

    List<Room> findByHotelIdAndTypeAndStatus(Long hotelId, RoomType type, RoomStatus status);

    List<Room> findByStatus(RoomStatus status);

    List<Room> findByTypeAndStatus(RoomType type, RoomStatus status);

    boolean existsByHotelIdAndRoomNumber(Long hotelId, String roomNumber);
}
