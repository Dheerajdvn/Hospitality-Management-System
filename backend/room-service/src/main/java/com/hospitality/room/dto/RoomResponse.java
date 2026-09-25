package com.hospitality.room.dto;

import com.hospitality.room.entity.RoomStatus;
import com.hospitality.room.entity.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long hotelId;
    private String roomNumber;
    private RoomType type;
    private RoomStatus status;
    private BigDecimal basePrice;
    private Integer capacity;
    private Integer floorNumber;
    private String description;
    private Set<String> amenities;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
