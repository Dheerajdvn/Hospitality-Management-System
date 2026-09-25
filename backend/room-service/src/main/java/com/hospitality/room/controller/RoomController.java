package com.hospitality.room.controller;

import com.hospitality.room.dto.ApiResponse;
import com.hospitality.room.dto.RoomRequest;
import com.hospitality.room.dto.RoomResponse;
import com.hospitality.room.dto.RoomStatusUpdateRequest;
import com.hospitality.room.entity.RoomStatusLog;
import com.hospitality.room.entity.RoomType;
import com.hospitality.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@Valid @RequestBody RoomRequest request) {
        RoomResponse response = roomService.createRoom(request);
        return new ResponseEntity<>(
                ApiResponse.success("Room created successfully", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable("id") Long id) {
        RoomResponse response = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success("Room details retrieved", response));
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByHotelId(@PathVariable("hotelId") Long hotelId) {
        List<RoomResponse> rooms = roomService.getRoomsByHotelId(hotelId);
        return ResponseEntity.ok(ApiResponse.success("Hotel rooms retrieved", rooms));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAvailableRooms(
            @RequestParam(value = "hotelId", required = false) Long hotelId,
            @RequestParam(value = "type", required = false) RoomType type) {
        List<RoomResponse> rooms = roomService.getAvailableRooms(hotelId, type);
        return ResponseEntity.ok(ApiResponse.success("Available rooms retrieved", rooms));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable("id") Long id,
            @Valid @RequestBody RoomRequest request) {
        RoomResponse response = roomService.updateRoom(id, request);
        return ResponseEntity.ok(ApiResponse.success("Room updated successfully", response));
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoomStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody RoomStatusUpdateRequest request) {
        RoomResponse response = roomService.updateRoomStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Room status updated successfully", response));
    }

    /**
     * Inter-service endpoint invoked by Booking Service before reserving a room.
     */
    @GetMapping("/{id}/available")
    public ResponseEntity<ApiResponse<Boolean>> checkRoomAvailable(@PathVariable("id") Long id) {
        boolean available = roomService.checkRoomAvailable(id);
        return ResponseEntity.ok(ApiResponse.success(
                available ? "Room is available" : "Room is not available",
                available
        ));
    }

    @GetMapping("/{id}/status-logs")
    public ResponseEntity<ApiResponse<List<RoomStatusLog>>> getRoomStatusLogs(@PathVariable("id") Long id) {
        List<RoomStatusLog> logs = roomService.getRoomStatusLogs(id);
        return ResponseEntity.ok(ApiResponse.success("Room status audit history retrieved", logs));
    }
}
