package com.hospitality.room.dto;

import com.hospitality.room.entity.RoomStatus;
import com.hospitality.room.entity.RoomType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {

    @NotNull(message = "Hotel ID is required")
    private Long hotelId;

    @NotBlank(message = "Room number is required")
    @Size(max = 20, message = "Room number cannot exceed 20 characters")
    private String roomNumber;

    @NotNull(message = "Room type is required")
    private RoomType type;

    private RoomStatus status; // Optional in create request, defaults to AVAILABLE

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be greater than zero")
    private BigDecimal basePrice;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1 guest")
    @Max(value = 10, message = "Capacity cannot exceed 10 guests")
    private Integer capacity;

    private Integer floorNumber;

    private String description;

    @Builder.Default
    private Set<String> amenities = new HashSet<>();
}
