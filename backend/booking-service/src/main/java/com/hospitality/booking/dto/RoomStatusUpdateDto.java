package com.hospitality.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomStatusUpdateDto {

    private String newStatus;
    private String changedBy;
    private String reason;
}
