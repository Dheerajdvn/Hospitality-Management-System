package com.hospitality.room.service;

import com.hospitality.room.client.HotelServiceClientDelegate;
import com.hospitality.room.dto.ApiResponse;
import com.hospitality.room.dto.RoomRequest;
import com.hospitality.room.dto.RoomResponse;
import com.hospitality.room.dto.RoomStatusUpdateRequest;
import com.hospitality.room.entity.Room;
import com.hospitality.room.entity.RoomStatus;
import com.hospitality.room.entity.RoomStatusLog;
import com.hospitality.room.entity.RoomType;
import com.hospitality.room.exception.BadRequestException;
import com.hospitality.room.exception.ResourceNotFoundException;
import com.hospitality.room.repository.RoomRepository;
import com.hospitality.room.repository.RoomStatusLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomStatusLogRepository roomStatusLogRepository;
    private final HotelServiceClientDelegate hotelClientDelegate;

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        log.info("Creating room number '{}' for hotel ID {}", request.getRoomNumber(), request.getHotelId());

        // Validate hotel existence via Feign client
        validateHotelExistsAndActive(request.getHotelId());

        // Check if room number already exists in this hotel
        if (roomRepository.existsByHotelIdAndRoomNumber(request.getHotelId(), request.getRoomNumber())) {
            throw new BadRequestException("Room number '" + request.getRoomNumber() + 
                    "' already exists in hotel ID " + request.getHotelId());
        }

        RoomStatus initialStatus = request.getStatus() != null ? request.getStatus() : RoomStatus.AVAILABLE;

        Room room = Room.builder()
                .hotelId(request.getHotelId())
                .roomNumber(request.getRoomNumber())
                .type(request.getType())
                .status(initialStatus)
                .basePrice(request.getBasePrice())
                .capacity(request.getCapacity())
                .floorNumber(request.getFloorNumber())
                .description(request.getDescription())
                .amenities(request.getAmenities() != null ? new HashSet<>(request.getAmenities()) : new HashSet<>())
                .build();

        Room savedRoom = roomRepository.save(room);

        // Record initial status creation audit log
        RoomStatusLog statusLog = RoomStatusLog.builder()
                .roomId(savedRoom.getId())
                .previousStatus(null)
                .newStatus(initialStatus)
                .changedBy("SYSTEM")
                .reason("Initial room registration")
                .build();
        roomStatusLogRepository.save(statusLog);

        log.info("Room created successfully with ID: {}", savedRoom.getId());
        return mapToResponse(savedRoom);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        Room room = findRoomByIdOrThrow(id);
        return mapToResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByHotelId(Long hotelId) {
        return roomRepository.findByHotelId(hotelId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(Long hotelId, RoomType type) {
        List<Room> rooms;
        if (hotelId != null && type != null) {
            rooms = roomRepository.findByHotelIdAndTypeAndStatus(hotelId, type, RoomStatus.AVAILABLE);
        } else if (hotelId != null) {
            rooms = roomRepository.findByHotelIdAndStatus(hotelId, RoomStatus.AVAILABLE);
        } else if (type != null) {
            rooms = roomRepository.findByTypeAndStatus(type, RoomStatus.AVAILABLE);
        } else {
            rooms = roomRepository.findByStatus(RoomStatus.AVAILABLE);
        }

        return rooms.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        log.info("Updating room ID: {}", id);
        Room room = findRoomByIdOrThrow(id);

        // If hotel or room number changed, check uniqueness
        if (!room.getRoomNumber().equalsIgnoreCase(request.getRoomNumber()) ||
                !room.getHotelId().equals(request.getHotelId())) {
            validateHotelExistsAndActive(request.getHotelId());
            if (roomRepository.existsByHotelIdAndRoomNumber(request.getHotelId(), request.getRoomNumber())) {
                throw new BadRequestException("Room number '" + request.getRoomNumber() + 
                        "' already exists in hotel ID " + request.getHotelId());
            }
            room.setHotelId(request.getHotelId());
            room.setRoomNumber(request.getRoomNumber());
        }

        room.setType(request.getType());
        room.setBasePrice(request.getBasePrice());
        room.setCapacity(request.getCapacity());
        room.setFloorNumber(request.getFloorNumber());
        room.setDescription(request.getDescription());
        if (request.getAmenities() != null) {
            room.setAmenities(new HashSet<>(request.getAmenities()));
        }

        Room updatedRoom = roomRepository.save(room);
        log.info("Room ID {} updated successfully", id);
        return mapToResponse(updatedRoom);
    }

    @Override
    @Transactional
    public RoomResponse updateRoomStatus(Long id, RoomStatusUpdateRequest request) {
        log.info("Updating status for room ID {} to {}", id, request.getNewStatus());
        Room room = findRoomByIdOrThrow(id);

        RoomStatus previousStatus = room.getStatus();
        if (previousStatus != request.getNewStatus()) {
            room.setStatus(request.getNewStatus());
            Room updatedRoom = roomRepository.save(room);

            // Record status audit log
            RoomStatusLog statusLog = RoomStatusLog.builder()
                    .roomId(updatedRoom.getId())
                    .previousStatus(previousStatus)
                    .newStatus(request.getNewStatus())
                    .changedBy(request.getChangedBy() != null ? request.getChangedBy() : "STAFF")
                    .reason(request.getReason() != null ? request.getReason() : "Operational status update")
                    .build();
            roomStatusLogRepository.save(statusLog);

            log.info("Room ID {} status changed from {} to {}", id, previousStatus, request.getNewStatus());
            return mapToResponse(updatedRoom);
        }

        return mapToResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkRoomAvailable(Long id) {
        return roomRepository.findById(id)
                .map(room -> RoomStatus.AVAILABLE.equals(room.getStatus()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomStatusLog> getRoomStatusLogs(Long roomId) {
        findRoomByIdOrThrow(roomId); // verify room exists
        return roomStatusLogRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
    }

    private Room findRoomByIdOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + id));
    }

    private void validateHotelExistsAndActive(Long hotelId) {
        boolean isActive = hotelClientDelegate.validateHotelActive(hotelId);
        if (!isActive) {
            throw new BadRequestException("Hotel with ID " + hotelId + " does not exist or is inactive");
        }
    }

    private RoomResponse mapToResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .hotelId(room.getHotelId())
                .roomNumber(room.getRoomNumber())
                .type(room.getType())
                .status(room.getStatus())
                .basePrice(room.getBasePrice())
                .capacity(room.getCapacity())
                .floorNumber(room.getFloorNumber())
                .description(room.getDescription())
                .amenities(room.getAmenities() != null ? new HashSet<>(room.getAmenities()) : new HashSet<>())
                .version(room.getVersion())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
