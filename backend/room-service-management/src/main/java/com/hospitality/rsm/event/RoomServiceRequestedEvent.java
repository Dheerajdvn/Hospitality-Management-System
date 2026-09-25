package com.hospitality.rsm.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomServiceRequestedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNumber;
    private String kotNumber;
    private Long bookingId;
    private Long hotelId;
    private Long roomId;
    private String roomNumber;
    private BigDecimal totalAmount;
    private String status;
    private List<OrderedItemEventDto> items;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderedItemEventDto implements Serializable {
        private Long foodItemId;
        private String foodItemName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
