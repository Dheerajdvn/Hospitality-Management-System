package com.hospitality.rsm.client;

import com.hospitality.rsm.dto.ApiResponse;
import com.hospitality.rsm.dto.BatchFoodLookupRequest;
import com.hospitality.rsm.dto.FoodItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "food-service", url = "${services.food-service.url}")
public interface FoodServiceClient {

    @PostMapping("/api/v1/food/batch")
    ApiResponse<List<FoodItemDto>> getFoodItemsBatch(@RequestBody BatchFoodLookupRequest request);
}
