package com.hospitality.hotel.service;

import com.hospitality.hotel.dto.HotelRequest;
import com.hospitality.hotel.dto.HotelResponse;
import com.hospitality.hotel.dto.PageResponse;
import com.hospitality.hotel.entity.Hotel;
import com.hospitality.hotel.exception.ResourceNotFoundException;
import com.hospitality.hotel.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private HotelServiceImpl hotelService;

    private Hotel sampleHotel;

    @BeforeEach
    void setUp() {
        sampleHotel = Hotel.builder()
                .id(1L)
                .name("Grand Palace")
                .description("Luxury stay")
                .city("New York")
                .state("NY")
                .country("USA")
                .address("500 5th Ave")
                .starRating(5.0)
                .phoneNumber("+1-212-555-0199")
                .email("info@grandpalace.com")
                .amenities(Set.of("WIFI", "SPA"))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create a hotel successfully and evict search caches")
    void testCreateHotelSuccess() {
        HotelRequest request = HotelRequest.builder()
                .name("Grand Palace")
                .city("New York")
                .country("USA")
                .address("500 5th Ave")
                .starRating(5.0)
                .amenities(Set.of("WIFI", "SPA"))
                .build();

        when(hotelRepository.save(any(Hotel.class))).thenReturn(sampleHotel);
        when(cacheManager.getCache("hotel_search")).thenReturn(cache);

        HotelResponse response = hotelService.createHotel(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Grand Palace");
        assertThat(response.getCity()).isEqualTo("New York");

        verify(hotelRepository, times(1)).save(any(Hotel.class));
        verify(cache, times(1)).clear();
    }

    @Test
    @DisplayName("Should retrieve hotel by ID successfully")
    void testGetHotelByIdSuccess() {
        when(hotelRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(sampleHotel));

        HotelResponse response = hotelService.getHotelById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Grand Palace");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when hotel ID does not exist")
    void testGetHotelByIdNotFound() {
        when(hotelRepository.findByIdAndIsActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelService.getHotelById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Hotel not found with ID");
    }

    @Test
    @DisplayName("Should search hotels with city filter and pagination")
    void testSearchHotels() {
        Page<Hotel> page = new PageImpl<>(List.of(sampleHotel));
        when(hotelRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<HotelResponse> response = hotelService.searchHotels(
                "New York", null, null, 0, 10, "id", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getCity()).isEqualTo("New York");
        assertThat(response.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should soft-deactivate hotel and evict search caches")
    void testDeleteHotel() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        when(hotelRepository.save(any(Hotel.class))).thenReturn(sampleHotel);
        when(cacheManager.getCache("hotel_search")).thenReturn(cache);

        hotelService.deleteHotel(1L);

        assertThat(sampleHotel.getIsActive()).isFalse();
        verify(hotelRepository, times(1)).save(sampleHotel);
        verify(cache, times(1)).clear();
    }

    @Test
    @DisplayName("Should validate active hotel status")
    void testValidateHotelActive() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));

        boolean isActive = hotelService.validateHotelActive(1L);

        assertThat(isActive).isTrue();
    }
}
