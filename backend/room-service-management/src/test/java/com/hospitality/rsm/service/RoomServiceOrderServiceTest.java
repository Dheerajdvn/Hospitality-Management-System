package com.hospitality.rsm.service;

import com.hospitality.rsm.client.BookingServiceClient;
import com.hospitality.rsm.client.FoodServiceClientDelegate;
import com.hospitality.rsm.dto.*;
import com.hospitality.rsm.entity.RoomServiceOrder;
import com.hospitality.rsm.entity.RoomServiceOrderItem;
import com.hospitality.rsm.entity.RoomServiceOrderStatus;
import com.hospitality.rsm.exception.BadRequestException;
import com.hospitality.rsm.exception.ResourceNotFoundException;
import com.hospitality.rsm.publisher.RoomServiceEventPublisher;
import com.hospitality.rsm.repository.RoomServiceOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceOrderServiceTest {

    @Mock
    private RoomServiceOrderRepository orderRepository;

    @Mock
    private FoodServiceClientDelegate foodServiceClientDelegate;

    @Mock
    private BookingServiceClient bookingServiceClient;

    @Mock
    private RoomServiceEventPublisher eventPublisher;

    @InjectMocks
    private RoomServiceOrderServiceImpl orderService;


    private CreateOrderRequest sampleRequest;
    private FoodItemDto foodItem1;
    private FoodItemDto foodItem2;
    private RoomServiceOrder sampleOrder;

    @BeforeEach
    void setUp() {
        sampleRequest = CreateOrderRequest.builder()
                .bookingId(101L)
                .hotelId(1L)
                .roomId(201L)
                .roomNumber("101")
                .items(List.of(
                        OrderItemRequest.builder().foodItemId(1L).quantity(2).notes("Extra spicy").build(),
                        OrderItemRequest.builder().foodItemId(2L).quantity(1).notes("No butter").build()
                ))
                .specialInstructions("Please ring bell twice")
                .build();

        foodItem1 = FoodItemDto.builder()
                .id(1L)
                .hotelId(1L)
                .name("Paneer Tikka")
                .price(new BigDecimal("350.00"))
                .isAvailable(true)
                .preparationTimeMinutes(20)
                .build();

        foodItem2 = FoodItemDto.builder()
                .id(2L)
                .hotelId(1L)
                .name("Dal Makhani")
                .price(new BigDecimal("380.00"))
                .isAvailable(true)
                .preparationTimeMinutes(15)
                .build();

        sampleOrder = RoomServiceOrder.builder()
                .id(1L)
                .orderNumber("RSO-20260925-TEST1")
                .kotNumber("KOT-1001")
                .bookingId(101L)
                .hotelId(1L)
                .roomId(201L)
                .roomNumber("101")
                .status(RoomServiceOrderStatus.ORDERED)
                .subtotal(new BigDecimal("1080.00"))
                .taxAmount(new BigDecimal("54.00"))
                .deliveryFee(new BigDecimal("30.00"))
                .totalAmount(new BigDecimal("1164.00"))
                .estimatedDeliveryMinutes(35)
                .orderedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should successfully create a room service order with valid items and calculations via resilient delegate")
    void createOrder_Success() {
        BookingDto bookingDto = BookingDto.builder()
                .id(101L)
                .hotelId(1L)
                .status("CONFIRMED")
                .build();

        lenient().when(bookingServiceClient.getBookingById(101L)).thenReturn(ApiResponse.success("Booking found", bookingDto));
        when(foodServiceClientDelegate.getFoodItemsBatch(any(BatchFoodLookupRequest.class)))
                .thenReturn(List.of(foodItem1, foodItem2));

        when(orderRepository.save(any(RoomServiceOrder.class))).thenAnswer(invocation -> {
            RoomServiceOrder order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        OrderResponse response = orderService.createOrder(sampleRequest);

        assertNotNull(response);
        assertEquals(RoomServiceOrderStatus.ORDERED, response.getStatus());
        // Subtotal: (350 * 2) + (380 * 1) = 700 + 380 = 1080.00
        assertEquals(new BigDecimal("1080.00"), response.getSubtotal());
        // Tax 5%: 1080 * 0.05 = 54.00
        assertEquals(new BigDecimal("54.00"), response.getTaxAmount());
        // Delivery fee: 30.00
        assertEquals(new BigDecimal("30.00"), response.getDeliveryFee());
        // Total: 1080 + 54 + 30 = 1164.00
        assertEquals(new BigDecimal("1164.00"), response.getTotalAmount());
        // Max prep time: 20 + 15 buffer = 35 mins
        assertEquals(35, response.getEstimatedDeliveryMinutes());
        assertEquals(2, response.getItems().size());
        verify(orderRepository, times(1)).save(any(RoomServiceOrder.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when a food item is out of stock / unavailable")
    void createOrder_FoodItemUnavailable() {
        foodItem1.setIsAvailable(false);
        when(foodServiceClientDelegate.getFoodItemsBatch(any(BatchFoodLookupRequest.class)))
                .thenReturn(List.of(foodItem1, foodItem2));

        assertThrows(BadRequestException.class, () -> orderService.createOrder(sampleRequest));
        verify(orderRepository, never()).save(any(RoomServiceOrder.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when a food item is missing from Food Service")
    void createOrder_FoodItemNotFound() {
        when(foodServiceClientDelegate.getFoodItemsBatch(any(BatchFoodLookupRequest.class)))
                .thenReturn(List.of(foodItem1)); // Missing item 2

        assertThrows(BadRequestException.class, () -> orderService.createOrder(sampleRequest));
        verify(orderRepository, never()).save(any(RoomServiceOrder.class));
    }

    @Test
    @DisplayName("Should successfully retrieve order by ID")
    void getOrderById_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("RSO-20260925-TEST1", response.getOrderNumber());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when order does not exist")
    void getOrderById_NotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(999L));
    }

    @Test
    @DisplayName("Should successfully transition state: ORDERED -> PREPARING -> OUT_FOR_DELIVERY -> DELIVERED")
    void updateOrderStatus_SuccessLifecycle() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(RoomServiceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. ORDERED -> PREPARING
        OrderResponse preparing = orderService.updateOrderStatus(1L, RoomServiceOrderStatus.PREPARING);
        assertEquals(RoomServiceOrderStatus.PREPARING, preparing.getStatus());

        // 2. PREPARING -> OUT_FOR_DELIVERY
        OrderResponse outForDelivery = orderService.updateOrderStatus(1L, RoomServiceOrderStatus.OUT_FOR_DELIVERY);
        assertEquals(RoomServiceOrderStatus.OUT_FOR_DELIVERY, outForDelivery.getStatus());

        // 3. OUT_FOR_DELIVERY -> DELIVERED
        OrderResponse delivered = orderService.updateOrderStatus(1L, RoomServiceOrderStatus.DELIVERED);
        assertEquals(RoomServiceOrderStatus.DELIVERED, delivered.getStatus());
        assertNotNull(delivered.getDeliveredAt());
    }

    @Test
    @DisplayName("Should throw BadRequestException on invalid state transition (ORDERED -> DELIVERED)")
    void updateOrderStatus_InvalidTransition() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

        assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(1L, RoomServiceOrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Should throw BadRequestException when attempting to update order in terminal DELIVERED status")
    void updateOrderStatus_TerminalState() {
        sampleOrder.setStatus(RoomServiceOrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

        assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(1L, RoomServiceOrderStatus.PREPARING));
    }

    @Test
    @DisplayName("Should successfully cancel order when in ORDERED status")
    void cancelOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(RoomServiceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse cancelled = orderService.cancelOrder(1L, "Guest changed mind");

        assertEquals(RoomServiceOrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Guest changed mind", cancelled.getCancellationReason());
    }

    @Test
    @DisplayName("Should throw BadRequestException when cancelling order that is already in PREPARING status")
    void cancelOrder_FailureWhenPreparing() {
        sampleOrder.setStatus(RoomServiceOrderStatus.PREPARING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

        assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(1L, "Guest changed mind"));
    }
}
