package com.hospitality.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_items", indexes = {
        @Index(name = "idx_food_hotel_id", columnList = "hotel_id"),
        @Index(name = "idx_food_category", columnList = "category"),
        @Index(name = "idx_food_available", columnList = "is_available")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private FoodCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_type", nullable = false, length = 30)
    private DietaryType dietaryType;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "preparation_time_minutes", nullable = false)
    @Builder.Default
    private Integer preparationTimeMinutes = 20;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isAvailable == null) {
            this.isAvailable = true;
        }
        if (this.preparationTimeMinutes == null) {
            this.preparationTimeMinutes = 20;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
