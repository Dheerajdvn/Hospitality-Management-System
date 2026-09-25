package com.hospitality.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUpdateRequest {

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String idProofType;
    private String idProofNumber;
    private AddressDto address;
    private String preferences;
    private Boolean isActive;
}
