package com.hospitality.hotel.controller;

import com.hospitality.hotel.dto.ApiResponse;
import com.hospitality.hotel.dto.HotelRequest;
import com.hospitality.hotel.dto.HotelResponse;
import com.hospitality.hotel.dto.PageResponse;
import com.hospitality.hotel.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<HotelResponse>>> searchHotels(
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "minRating", required = false) Double minRating,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {
        PageResponse<HotelResponse> response = hotelService.searchHotels(city, name, minRating, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Hotels retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponse>> getHotelById(@PathVariable("id") Long id) {
        HotelResponse response = hotelService.getHotelById(id);
        return ResponseEntity.ok(ApiResponse.success("Hotel details retrieved", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HotelResponse>> createHotel(@Valid @RequestBody HotelRequest request) {
        HotelResponse response = hotelService.createHotel(request);
        return new ResponseEntity<>(
                ApiResponse.success("Hotel created successfully", response),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponse>> updateHotel(
            @PathVariable("id") Long id,
            @Valid @RequestBody HotelRequest request) {
        HotelResponse response = hotelService.updateHotel(id, request);
        return ResponseEntity.ok(ApiResponse.success("Hotel updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHotel(@PathVariable("id") Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.ok(ApiResponse.success("Hotel deactivated successfully", null));
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateHotelActive(@PathVariable("id") Long id) {
        boolean isActive = hotelService.validateHotelActive(id);
        return ResponseEntity.ok(ApiResponse.success(
                isActive ? "Hotel is active" : "Hotel not found or inactive",
                isActive
        ));
    }
}
