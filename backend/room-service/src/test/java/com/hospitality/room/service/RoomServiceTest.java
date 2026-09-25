package com.hospitality.room.service;

import com.hospitality.room.client.HotelServiceClientDelegate;
import com.hospitality.room.dto.ApiResponse;
import com.hospitality.room.dto.RoomRequest;
import com.hospitality.room.dto.RoomResponse;
import com.hospitality.room.dto.RoomStatusUpdateRequest;
import com.hospitality.room.entity.Room;
import com.hospitality.room.entity.RoomStatus;
import com.hospitality.room.entity.RoomStatusLog;
import com.hospitality.room.entity.RoomType;
import com.hospitality.room.exception.BadRequestException;
import com.hospitality.room.exception.ResourceNotFoundException;
import com.hospitality.room.repository.RoomRepository;
import com.hospitality.room.repository.RoomStatusLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomStatusLogRepository roomStatusLogRepository;

    @Mock
    private HotelServiceClientDelegate hotelClientDelegate;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room sampleRoom;
    private RoomRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRoom = Room.builder()
                .id(1L)
                .hotelId(1L)
                .roomNumber("101")
                .type(RoomType.DELUXE)
                .status(RoomStatus.AVAILABLE)
                .basePrice(new BigDecimal("5000.00"))
                .capacity(2)
                .floorNumber(1)
                .description("Luxury room")
                .amenities(Set.of("WiFi", "AC"))
                .build();

        sampleRequest = RoomRequest.builder()
                .hotelId(1L)
                .roomNumber("101")
                .type(RoomType.DELUXE)
                .status(RoomStatus.AVAILABLE)
                .basePrice(new BigDecimal("5000.00"))
                .capacity(2)
                .floorNumber(1)
                .description("Luxury room")
                .amenities(Set.of("WiFi", "AC"))
                .build();
    }

    @Test
    @DisplayName("Should successfully create a room and save initial audit log via resilient delegate")
    void testCreateRoom_Success() {
        when(hotelClientDelegate.validateHotelActive(1L)).thenReturn(true);
        when(roomRepository.existsByHotelIdAndRoomNumber(1L, "101")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(sampleRoom);
        when(roomStatusLogRepository.save(any(RoomStatusLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomResponse response = roomService.createRoom(sampleRequest);

        assertNotNull(response);
        assertEquals("101", response.getRoomNumber());
        assertEquals(RoomStatus.AVAILABLE, response.getStatus());
        assertEquals(new BigDecimal("5000.00"), response.getBasePrice());

        verify(hotelClientDelegate).validateHotelActive(1L);
        verify(roomRepository).save(any(Room.class));
        verify(roomStatusLogRepository).save(any(RoomStatusLog.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if hotel does not exist or is inactive")
    void testCreateRoom_InactiveHotel_ThrowsBadRequest() {
        when(hotelClientDelegate.validateHotelActive(1L)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                roomService.createRoom(sampleRequest));

        assertTrue(exception.getMessage().contains("does not exist or is inactive"));
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if room number already exists in hotel")
    void testCreateRoom_DuplicateRoomNumber_ThrowsBadRequest() {
        when(hotelClientDelegate.validateHotelActive(1L)).thenReturn(true);
        when(roomRepository.existsByHotelIdAndRoomNumber(1L, "101")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                roomService.createRoom(sampleRequest));

        assertTrue(exception.getMessage().contains("already exists"));
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Should retrieve room by ID successfully")
    void testGetRoomById_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));

        RoomResponse response = roomService.getRoomById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("101", response.getRoomNumber());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when room ID does not exist")
    void testGetRoomById_NotFound_ThrowsException() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roomService.getRoomById(999L));
    }

    @Test
    @DisplayName("Should transition room status and create audit log")
    void testUpdateRoomStatus_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(sampleRoom);

        RoomStatusUpdateRequest updateRequest = RoomStatusUpdateRequest.builder()
                .newStatus(RoomStatus.BOOKED)
                .changedBy("BOOKING_SERVICE")
                .reason("Reservation confirmed")
                .build();

        RoomResponse response = roomService.updateRoomStatus(1L, updateRequest);

        assertNotNull(response);
        assertEquals(RoomStatus.BOOKED, response.getStatus());

        verify(roomStatusLogRepository).save(argThat(log ->
                log.getPreviousStatus() == RoomStatus.AVAILABLE &&
                log.getNewStatus() == RoomStatus.BOOKED &&
                "BOOKING_SERVICE".equals(log.getChangedBy()) &&
                "Reservation confirmed".equals(log.getReason())
        ));
    }

    @Test
    @DisplayName("Should check room availability correctly")
    void testCheckRoomAvailable() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        assertTrue(roomService.checkRoomAvailable(1L));

        sampleRoom.setStatus(RoomStatus.OCCUPIED);
        assertFalse(roomService.checkRoomAvailable(1L));

        when(roomRepository.findById(2L)).thenReturn(Optional.empty());
        assertFalse(roomService.checkRoomAvailable(2L));
    }

    @Test
    @DisplayName("Should retrieve available rooms filtered by hotel and type")
    void testGetAvailableRooms_Filtered() {
        when(roomRepository.findByHotelIdAndTypeAndStatus(1L, RoomType.DELUXE, RoomStatus.AVAILABLE))
                .thenReturn(List.of(sampleRoom));

        List<RoomResponse> available = roomService.getAvailableRooms(1L, RoomType.DELUXE);

        assertNotNull(available);
        assertEquals(1, available.size());
        assertEquals("101", available.get(0).getRoomNumber());
    }
}
