package com.hospitality.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "hotels", indexes = {
        @Index(name = "idx_hotel_city", columnList = "city"),
        @Index(name = "idx_hotel_rating", columnList = "star_rating"),
        @Index(name = "idx_hotel_name", columnList = "name"),
        @Index(name = "idx_hotel_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 60)
    private String city;

    @Column(length = 60)
    private String state;

    @Column(nullable = false, length = 60)
    private String country;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "star_rating", nullable = false)
    private Double starRating; // e.g. 4.5, 5.0

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(length = 100)
    private String email;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_amenities", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "amenity")
    @Builder.Default
    private Set<String> amenities = new HashSet<>();

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
