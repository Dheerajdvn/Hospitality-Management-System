package com.hospitality.booking.client;

import com.hospitality.booking.dto.ApiResponse;
import com.hospitality.booking.dto.RoomDetailResponse;
import com.hospitality.booking.dto.RoomStatusUpdateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "room-service", url = "${services.room-service.url}")
public interface RoomClient {

    @GetMapping("/api/v1/rooms/{id}")
    ApiResponse<RoomDetailResponse> getRoomById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/rooms/{id}/available")
    ApiResponse<Boolean> checkRoomAvailable(@PathVariable("id") Long id);

    @org.springframework.web.bind.annotation.PutMapping("/api/v1/rooms/{id}/status")
    ApiResponse<Object> updateRoomStatus(@PathVariable("id") Long id, @RequestBody RoomStatusUpdateDto request);
}
