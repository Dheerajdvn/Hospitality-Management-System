package com.hospitality.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Column(name = "street_address", length = 150)
    private String street;

    @Column(length = 60)
    private String city;

    @Column(length = 60)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 60)
    private String country;
}
