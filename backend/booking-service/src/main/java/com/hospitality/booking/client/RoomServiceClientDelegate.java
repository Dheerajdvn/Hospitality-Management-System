package com.hospitality.booking.client;

import com.hospitality.booking.dto.ApiResponse;
import com.hospitality.booking.dto.RoomDetailResponse;
import com.hospitality.booking.dto.RoomStatusUpdateDto;
import com.hospitality.booking.exception.BadRequestException;
import com.hospitality.booking.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resilient delegate wrapper for RoomClient OpenFeign calls in booking-service.
 * Enforces Resilience4j Circuit Breaker and Retry semantics with fallback behavior.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomServiceClientDelegate {

    private final RoomClient roomClient;

    @CircuitBreaker(name = "roomServiceCircuitBreaker", fallbackMethod = "fetchRoomFallback")
    @Retry(name = "roomServiceRetry")
    public RoomDetailResponse getRoomById(Long roomId) {
        log.info("Calling room-service for room ID: {}", roomId);
        ApiResponse<RoomDetailResponse> response = roomClient.getRoomById(roomId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BadRequestException("Room with ID " + roomId + " not found in room service");
        }
        return response.getData();
    }

    public RoomDetailResponse fetchRoomFallback(Long roomId, CallNotPermittedException ex) {
        log.error("Circuit Breaker [roomServiceCircuitBreaker] is OPEN for room ID: {}. Reason: {}", roomId, ex.getMessage());
        throw new ServiceUnavailableException("Room service is temporarily unavailable (circuit breaker is OPEN). Please try again shortly.");
    }

    public RoomDetailResponse fetchRoomFallback(Long roomId, Throwable ex) {
        log.error("Fallback triggered for room ID {}: {}", roomId, ex.getMessage());
        if (ex instanceof BadRequestException) {
            throw (BadRequestException) ex;
        }
        throw new ServiceUnavailableException("Room service is currently unreachable: " + ex.getMessage());
    }

    @CircuitBreaker(name = "roomServiceCircuitBreaker", fallbackMethod = "updateRoomStatusFallback")
    @Retry(name = "roomServiceRetry")
    public void updateRoomStatus(Long roomId, RoomStatusUpdateDto dto) {
        log.info("Updating room status for room ID: {} to {}", roomId, dto.getNewStatus());
        roomClient.updateRoomStatus(roomId, dto);
    }

    public void updateRoomStatusFallback(Long roomId, RoomStatusUpdateDto dto, CallNotPermittedException ex) {
        log.error("Circuit Breaker is OPEN. Failed to update status of room ID {}: {}", roomId, ex.getMessage());
        throw new ServiceUnavailableException("Room service is temporarily unavailable (circuit breaker is OPEN). Status update failed.");
    }

    public void updateRoomStatusFallback(Long roomId, RoomStatusUpdateDto dto, Throwable ex) {
        log.error("Fallback triggered for updateRoomStatus on room ID {}: {}", roomId, ex.getMessage());
        throw new ServiceUnavailableException("Room service is currently unreachable for status update: " + ex.getMessage());
    }
}
