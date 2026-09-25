package com.hospitality.rsm.dto;

import com.hospitality.rsm.entity.RoomServiceOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private String kotNumber;
    private Long bookingId;
    private Long hotelId;
    private Long roomId;
    private String roomNumber;
    private RoomServiceOrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private Integer estimatedDeliveryMinutes;
    private String specialInstructions;
    private String cancellationReason;
    private LocalDateTime orderedAt;
    private LocalDateTime deliveredAt;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
