package com.hospitality.rsm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Hotel ID is required")
    private Long hotelId;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotBlank(message = "Room number is required")
    private String roomNumber;

    @NotEmpty(message = "Order must contain at least one food item")
    @Valid
    private List<OrderItemRequest> items;

    @Size(max = 500, message = "Special instructions cannot exceed 500 characters")
    private String specialInstructions;
}
