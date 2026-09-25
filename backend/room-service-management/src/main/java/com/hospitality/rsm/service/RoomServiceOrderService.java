package com.hospitality.rsm.service;

import com.hospitality.rsm.dto.CreateOrderRequest;
import com.hospitality.rsm.dto.OrderResponse;
import com.hospitality.rsm.entity.RoomServiceOrderStatus;

import java.util.List;

public interface RoomServiceOrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(Long id);

    OrderResponse getOrderByOrderNumber(String orderNumber);

    List<OrderResponse> getOrdersByBookingId(Long bookingId);

    List<OrderResponse> getOrdersByHotel(Long hotelId, RoomServiceOrderStatus status);

    OrderResponse updateOrderStatus(Long id, RoomServiceOrderStatus newStatus);

    OrderResponse cancelOrder(Long id, String reason);
}
