package com.hospitality.hotel.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelRequest {

    @NotBlank(message = "Hotel name is required")
    @Size(max = 120, message = "Hotel name cannot exceed 120 characters")
    private String name;

    private String description;

    @NotBlank(message = "City is required")
    private String city;

    private String state;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Address is required")
    private String address;

    private String postalCode;

    @NotNull(message = "Star rating is required")
    @DecimalMin(value = "1.0", message = "Minimum star rating is 1.0")
    @DecimalMax(value = "5.0", message = "Maximum star rating is 5.0")
    private Double starRating;

    private String phoneNumber;

    @Email(message = "Please provide a valid email format")
    private String email;

    private String imageUrl;

    private Set<String> amenities;
}
