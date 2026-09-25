package com.hospitality.rsm.controller;

import com.hospitality.rsm.dto.ApiResponse;
import com.hospitality.rsm.dto.CancelOrderRequest;
import com.hospitality.rsm.dto.CreateOrderRequest;
import com.hospitality.rsm.dto.OrderResponse;
import com.hospitality.rsm.entity.RoomServiceOrderStatus;
import com.hospitality.rsm.service.RoomServiceOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/room-service/orders")
@RequiredArgsConstructor
@Slf4j
public class RoomServiceOrderController {

    private final RoomServiceOrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room service order created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", response));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByOrderNumber(@PathVariable String orderNumber) {
        OrderResponse response = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", response));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByBookingId(@PathVariable Long bookingId) {
        List<OrderResponse> orders = orderService.getOrdersByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Orders for booking retrieved successfully", orders));
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByHotel(
            @PathVariable Long hotelId,
            @RequestParam(required = false) RoomServiceOrderStatus status) {
        List<OrderResponse> orders = orderService.getOrdersByHotel(hotelId, status);
        return ResponseEntity.ok(ApiResponse.success("Orders for hotel retrieved successfully", orders));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> patchOrderStatus(
            @PathVariable Long id,
            @RequestParam RoomServiceOrderStatus status) {
        OrderResponse updated = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Order status updated to " + status, updated));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> putOrderStatus(
            @PathVariable Long id,
            @RequestParam RoomServiceOrderStatus status) {
        OrderResponse updated = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Order status updated to " + status, updated));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest request) {
        String reason = (request != null && request.getReason() != null) ? request.getReason() : "Cancelled by guest";
        OrderResponse cancelled = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", cancelled));
    }
}
