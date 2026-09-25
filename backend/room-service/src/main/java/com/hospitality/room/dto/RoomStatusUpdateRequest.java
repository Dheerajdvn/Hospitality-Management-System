package com.hospitality.room.dto;

import com.hospitality.room.entity.RoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomStatusUpdateRequest {

    @NotNull(message = "New status is required")
    private RoomStatus newStatus;

    private String changedBy;

    private String reason;
}
