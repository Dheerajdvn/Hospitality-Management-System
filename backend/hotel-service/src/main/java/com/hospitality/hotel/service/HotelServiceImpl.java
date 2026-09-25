package com.hospitality.hotel.service;

import com.hospitality.hotel.dto.HotelRequest;
import com.hospitality.hotel.dto.HotelResponse;
import com.hospitality.hotel.dto.PageResponse;
import com.hospitality.hotel.entity.Hotel;
import com.hospitality.hotel.exception.ResourceNotFoundException;
import com.hospitality.hotel.repository.HotelRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public HotelResponse createHotel(HotelRequest request) {
        log.info("Creating new hotel: {}", request.getName());

        Hotel hotel = Hotel.builder()
                .name(request.getName())
                .description(request.getDescription())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .address(request.getAddress())
                .postalCode(request.getPostalCode())
                .starRating(request.getStarRating())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .imageUrl(request.getImageUrl())
                .amenities(request.getAmenities() != null ? new HashSet<>(request.getAmenities()) : new HashSet<>())
                .isActive(true)
                .build();

        Hotel saved = hotelRepository.save(hotel);
        evictSearchCaches();
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "hotel_details", key = "#id")
    public HotelResponse getHotelById(Long id) {
        log.info("CACHE MISS: Fetching hotel ID {} from PostgreSQL database", id);
        Hotel hotel = hotelRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + id));
        return mapToResponse(hotel);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "hotel_search", key = "{#city, #name, #minRating, #page, #size, #sortBy, #sortDir}")
    public PageResponse<HotelResponse> searchHotels(
            String city,
            String name,
            Double minRating,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        log.info("CACHE MISS: Searching hotels in database with city='{}', name='{}', minRating={}, page={}, size={}",
                city, name, minRating, page, size);

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Hotel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));

            if (city != null && !city.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase().trim() + "%"));
            }

            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase().trim() + "%"));
            }

            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("starRating"), minRating));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Hotel> hotelPage = hotelRepository.findAll(spec, pageable);

        List<HotelResponse> content = hotelPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<HotelResponse>builder()
                .content(content)
                .pageNo(hotelPage.getNumber())
                .pageSize(hotelPage.getSize())
                .totalElements(hotelPage.getTotalElements())
                .totalPages(hotelPage.getTotalPages())
                .last(hotelPage.isLast())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "hotel_details", key = "#id")
    public HotelResponse updateHotel(Long id, HotelRequest request) {
        log.info("Updating hotel ID: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + id));

        hotel.setName(request.getName());
        hotel.setDescription(request.getDescription());
        hotel.setCity(request.getCity());
        hotel.setState(request.getState());
        hotel.setCountry(request.getCountry());
        hotel.setAddress(request.getAddress());
        hotel.setPostalCode(request.getPostalCode());
        hotel.setStarRating(request.getStarRating());
        hotel.setPhoneNumber(request.getPhoneNumber());
        hotel.setEmail(request.getEmail());
        hotel.setImageUrl(request.getImageUrl());

        if (request.getAmenities() != null) {
            hotel.setAmenities(new HashSet<>(request.getAmenities()));
        }

        Hotel updated = hotelRepository.save(hotel);
        evictSearchCaches();
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "hotel_details", key = "#id")
    public void deleteHotel(Long id) {
        log.info("Soft-deactivating hotel ID: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + id));
        hotel.setIsActive(false);
        hotelRepository.save(hotel);
        evictSearchCaches();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateHotelActive(Long id) {
        return hotelRepository.findById(id)
                .map(Hotel::getIsActive)
                .orElse(false);
    }

    private void evictSearchCaches() {
        var cache = cacheManager.getCache("hotel_search");
        if (cache != null) {
            cache.clear();
            log.info("Invalidated 'hotel_search' Redis cache cluster");
        }
    }

    private HotelResponse mapToResponse(Hotel hotel) {
        return HotelResponse.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .description(hotel.getDescription())
                .city(hotel.getCity())
                .state(hotel.getState())
                .country(hotel.getCountry())
                .address(hotel.getAddress())
                .postalCode(hotel.getPostalCode())
                .starRating(hotel.getStarRating())
                .phoneNumber(hotel.getPhoneNumber())
                .email(hotel.getEmail())
                .imageUrl(hotel.getImageUrl())
                .amenities(hotel.getAmenities() != null ? new HashSet<>(hotel.getAmenities()) : new HashSet<>())
                .isActive(hotel.getIsActive())
                .createdAt(hotel.getCreatedAt())
                .build();
    }
}
