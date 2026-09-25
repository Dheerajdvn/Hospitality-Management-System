package com.hospitality.rsm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {

    private Long id;
    private String bookingNumber;
    private Long customerId;
    private Long hotelId;
    private Long roomId;
    private String status;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}
