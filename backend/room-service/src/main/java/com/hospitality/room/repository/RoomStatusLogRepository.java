package com.hospitality.room.repository;

import com.hospitality.room.entity.RoomStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomStatusLogRepository extends JpaRepository<RoomStatusLog, Long> {

    List<RoomStatusLog> findByRoomIdOrderByCreatedAtDesc(Long roomId);
}
