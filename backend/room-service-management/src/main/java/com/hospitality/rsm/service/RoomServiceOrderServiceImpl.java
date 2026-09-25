package com.hospitality.rsm.service;

import com.hospitality.rsm.client.BookingServiceClient;
import com.hospitality.rsm.client.FoodServiceClientDelegate;
import com.hospitality.rsm.dto.*;
import com.hospitality.rsm.entity.RoomServiceOrder;
import com.hospitality.rsm.entity.RoomServiceOrderItem;
import com.hospitality.rsm.entity.RoomServiceOrderStatus;
import com.hospitality.rsm.event.RoomServiceRequestedEvent;
import com.hospitality.rsm.exception.BadRequestException;
import com.hospitality.rsm.exception.ResourceNotFoundException;
import com.hospitality.rsm.publisher.RoomServiceEventPublisher;
import com.hospitality.rsm.repository.RoomServiceOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceOrderServiceImpl implements RoomServiceOrderService {

    private final RoomServiceOrderRepository orderRepository;
    private final FoodServiceClientDelegate foodServiceClientDelegate;
    private final BookingServiceClient bookingServiceClient;
    private final RoomServiceEventPublisher eventPublisher;


    private static final BigDecimal TAX_RATE = new BigDecimal("0.05"); // 5% GST on dining
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("30.00");
    private static final int DELIVERY_BUFFER_MINUTES = 15;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating room service order for booking ID {}, room {}, hotel ID {}",
                request.getBookingId(), request.getRoomNumber(), request.getHotelId());

        // 1. Validate Booking
        validateBooking(request.getBookingId(), request.getHotelId());

        // 2. Validate and fetch food items via Food Service batch endpoint
        List<Long> requestedItemIds = request.getItems().stream()
                .map(OrderItemRequest::getFoodItemId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, FoodItemDto> foodItemMap = validateAndFetchFoodItems(requestedItemIds, request.getHotelId());

        // 3. Calculate subtotal, delivery time, and build line items
        BigDecimal subtotal = BigDecimal.ZERO;
        int maxPrepTime = 0;
        List<RoomServiceOrderItem> lineItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            FoodItemDto foodItem = foodItemMap.get(itemReq.getFoodItemId());
            BigDecimal unitPrice = foodItem.getPrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            subtotal = subtotal.add(totalPrice);

            if (foodItem.getPreparationTimeMinutes() != null && foodItem.getPreparationTimeMinutes() > maxPrepTime) {
                maxPrepTime = foodItem.getPreparationTimeMinutes();
            }

            RoomServiceOrderItem lineItem = RoomServiceOrderItem.builder()
                    .foodItemId(foodItem.getId())
                    .foodItemName(foodItem.getName())
                    .unitPrice(unitPrice)
                    .quantity(itemReq.getQuantity())
                    .totalPrice(totalPrice)
                    .notes(itemReq.getNotes())
                    .build();

            lineItems.add(lineItem);
        }

        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount).add(DELIVERY_FEE);
        int estimatedDeliveryMinutes = maxPrepTime + DELIVERY_BUFFER_MINUTES;

        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNumber = "RSO-" + datePrefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String kotNumber = "KOT-" + (System.currentTimeMillis() % 100000);

        RoomServiceOrder order = RoomServiceOrder.builder()
                .orderNumber(orderNumber)
                .kotNumber(kotNumber)
                .bookingId(request.getBookingId())
                .hotelId(request.getHotelId())
                .roomId(request.getRoomId())
                .roomNumber(request.getRoomNumber())
                .status(RoomServiceOrderStatus.ORDERED)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .deliveryFee(DELIVERY_FEE)
                .totalAmount(totalAmount)
                .estimatedDeliveryMinutes(estimatedDeliveryMinutes)
                .specialInstructions(request.getSpecialInstructions())
                .orderedAt(LocalDateTime.now())
                .build();

        for (RoomServiceOrderItem item : lineItems) {
            order.addItem(item);
        }

        RoomServiceOrder saved = orderRepository.save(order);
        log.info("Successfully created room service order {} with KOT {}", saved.getOrderNumber(), saved.getKotNumber());

        List<RoomServiceRequestedEvent.OrderedItemEventDto> eventItems = saved.getItems().stream()
                .map(item -> RoomServiceRequestedEvent.OrderedItemEventDto.builder()
                        .foodItemId(item.getFoodItemId())
                        .foodItemName(item.getFoodItemName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        eventPublisher.publishRoomServiceRequested(RoomServiceRequestedEvent.builder()
                .orderId(saved.getId())
                .orderNumber(saved.getOrderNumber())
                .kotNumber(saved.getKotNumber())
                .bookingId(saved.getBookingId())
                .hotelId(saved.getHotelId())
                .roomId(saved.getRoomId())
                .roomNumber(saved.getRoomNumber())
                .totalAmount(saved.getTotalAmount())
                .status(saved.getStatus().name())
                .items(eventItems)
                .build());

        return mapToResponse(saved);

    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        RoomServiceOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room service order not found with ID: " + id));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        RoomServiceOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Room service order not found with order number: " + orderNumber));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByBookingId(Long bookingId) {
        return orderRepository.findByBookingIdOrderByCreatedAtDesc(bookingId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByHotel(Long hotelId, RoomServiceOrderStatus status) {
        List<RoomServiceOrder> orders;
        if (status != null) {
            orders = orderRepository.findByHotelIdAndStatusOrderByCreatedAtDesc(hotelId, status);
        } else {
            orders = orderRepository.findByHotelIdOrderByCreatedAtDesc(hotelId);
        }
        return orders.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, RoomServiceOrderStatus newStatus) {
        log.info("Updating status for order ID {} to {}", id, newStatus);

        RoomServiceOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room service order not found with ID: " + id));

        RoomServiceOrderStatus current = order.getStatus();

        // Terminal state check
        if (current == RoomServiceOrderStatus.DELIVERED || current == RoomServiceOrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status of an order that is already " + current);
        }

        // Validate state transitions
        boolean validTransition = false;
        switch (current) {
            case ORDERED -> validTransition = (newStatus == RoomServiceOrderStatus.PREPARING || newStatus == RoomServiceOrderStatus.CANCELLED);
            case PREPARING -> validTransition = (newStatus == RoomServiceOrderStatus.OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY -> validTransition = (newStatus == RoomServiceOrderStatus.DELIVERED);
            default -> validTransition = false;
        }

        if (!validTransition) {
            throw new BadRequestException("Invalid state transition from " + current + " to " + newStatus);
        }

        order.setStatus(newStatus);
        if (newStatus == RoomServiceOrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        RoomServiceOrder updated = orderRepository.save(order);
        log.info("Order ID {} status updated to {}", updated.getId(), updated.getStatus());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id, String reason) {
        log.info("Cancelling room service order ID: {}", id);

        RoomServiceOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room service order not found with ID: " + id));

        if (order.getStatus() != RoomServiceOrderStatus.ORDERED) {
            throw new BadRequestException("Order cannot be cancelled because it is in '" + order.getStatus() +
                    "' status. Cancellation is only permitted before food preparation has begun.");
        }

        order.setStatus(RoomServiceOrderStatus.CANCELLED);
        order.setCancellationReason(reason != null ? reason : "Cancelled by guest");

        RoomServiceOrder updated = orderRepository.save(order);
        log.info("Order ID {} successfully cancelled", updated.getId());
        return mapToResponse(updated);
    }

    private void validateBooking(Long bookingId, Long hotelId) {
        try {
            ApiResponse<BookingDto> response = bookingServiceClient.getBookingById(bookingId);
            if (response != null && response.isSuccess() && response.getData() != null) {
                BookingDto booking = response.getData();
                if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
                    throw new BadRequestException("Cannot place room service order for a cancelled booking (ID: " + bookingId + ")");
                }
                if (booking.getHotelId() != null && !booking.getHotelId().equals(hotelId)) {
                    throw new BadRequestException("Booking ID " + bookingId + " belongs to hotel ID " + booking.getHotelId() + ", not " + hotelId);
                }
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Booking service unreachable or error verifying booking ID {}: {}. Proceeding in resilient mode.", bookingId, ex.getMessage());
        }
    }

    private Map<Long, FoodItemDto> validateAndFetchFoodItems(List<Long> foodItemIds, Long hotelId) {
        List<FoodItemDto> items = foodServiceClientDelegate.getFoodItemsBatch(new BatchFoodLookupRequest(foodItemIds));
        Map<Long, FoodItemDto> itemMap = items.stream()
                .collect(Collectors.toMap(FoodItemDto::getId, item -> item));

        for (Long id : foodItemIds) {
            FoodItemDto item = itemMap.get(id);
            if (item == null) {
                throw new BadRequestException("Food item with ID " + id + " does not exist in catalog");
            }
            if (item.getHotelId() != null && !item.getHotelId().equals(hotelId)) {
                throw new BadRequestException("Food item '" + item.getName() + "' is not available at hotel ID " + hotelId);
            }
            if (Boolean.FALSE.equals(item.getIsAvailable())) {
                throw new BadRequestException("Food item '" + item.getName() + "' is currently unavailable / out of stock");
            }
        }
        return itemMap;
    }

    private OrderResponse mapToResponse(RoomServiceOrder order) {
        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(this::mapItemToResponse).collect(Collectors.toList())
                : List.of();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .kotNumber(order.getKotNumber())
                .bookingId(order.getBookingId())
                .hotelId(order.getHotelId())
                .roomId(order.getRoomId())
                .roomNumber(order.getRoomNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .deliveryFee(order.getDeliveryFee())
                .totalAmount(order.getTotalAmount())
                .estimatedDeliveryMinutes(order.getEstimatedDeliveryMinutes())
                .specialInstructions(order.getSpecialInstructions())
                .cancellationReason(order.getCancellationReason())
                .orderedAt(order.getOrderedAt())
                .deliveredAt(order.getDeliveredAt())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse mapItemToResponse(RoomServiceOrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .foodItemId(item.getFoodItemId())
                .foodItemName(item.getFoodItemName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .totalPrice(item.getTotalPrice())
                .notes(item.getNotes())
                .build();
    }
}
