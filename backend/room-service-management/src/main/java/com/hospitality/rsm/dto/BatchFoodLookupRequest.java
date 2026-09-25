package com.hospitality.rsm.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchFoodLookupRequest {

    @NotEmpty(message = "Food item IDs list cannot be empty")
    private List<Long> foodItemIds;
}
