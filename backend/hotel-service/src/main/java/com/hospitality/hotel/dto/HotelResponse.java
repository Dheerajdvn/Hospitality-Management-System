package com.hospitality.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private String city;
    private String state;
    private String country;
    private String address;
    private String postalCode;
    private Double starRating;
    private String phoneNumber;
    private String email;
    private String imageUrl;
    private Set<String> amenities;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
