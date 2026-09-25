package com.hospitality.room.service;

import com.hospitality.room.dto.RoomRequest;
import com.hospitality.room.dto.RoomResponse;
import com.hospitality.room.dto.RoomStatusUpdateRequest;
import com.hospitality.room.entity.RoomStatus;
import com.hospitality.room.entity.RoomStatusLog;
import com.hospitality.room.entity.RoomType;

import java.util.List;

public interface RoomService {

    RoomResponse createRoom(RoomRequest request);

    RoomResponse getRoomById(Long id);

    List<RoomResponse> getRoomsByHotelId(Long hotelId);

    List<RoomResponse> getAvailableRooms(Long hotelId, RoomType type);

    RoomResponse updateRoom(Long id, RoomRequest request);

    RoomResponse updateRoomStatus(Long id, RoomStatusUpdateRequest request);

    boolean checkRoomAvailable(Long id);

    List<RoomStatusLog> getRoomStatusLogs(Long roomId);
}
