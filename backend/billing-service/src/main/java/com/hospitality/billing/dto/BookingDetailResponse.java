package com.hospitality.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingDetailResponse {

    private Long id;
    private String bookingReference;
    private Long customerId;
    private Long roomId;
    private Long hotelId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfNights;
    private Integer numberOfGuests;
    private BigDecimal roomPricePerNight;
    private BigDecimal totalAmount;
    private String status;
    private String paymentStatus;
}
