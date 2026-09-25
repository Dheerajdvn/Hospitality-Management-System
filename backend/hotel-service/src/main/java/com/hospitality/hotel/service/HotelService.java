package com.hospitality.hotel.service;

import com.hospitality.hotel.dto.HotelRequest;
import com.hospitality.hotel.dto.HotelResponse;
import com.hospitality.hotel.dto.PageResponse;

public interface HotelService {

    HotelResponse createHotel(HotelRequest request);

    HotelResponse getHotelById(Long id);

    PageResponse<HotelResponse> searchHotels(
            String city,
            String name,
            Double minRating,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    HotelResponse updateHotel(Long id, HotelRequest request);

    void deleteHotel(Long id);

    boolean validateHotelActive(Long id);
}
