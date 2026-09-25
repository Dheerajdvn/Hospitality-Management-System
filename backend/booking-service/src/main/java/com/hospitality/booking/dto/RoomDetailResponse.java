package com.hospitality.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomDetailResponse {

    private Long id;
    private Long hotelId;
    private String roomNumber;
    private String type;
    private String status;
    private BigDecimal basePrice;
    private Integer capacity;
    private Integer floorNumber;
    private String description;
    private Set<String> amenities;
}
