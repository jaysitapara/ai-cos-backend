package com.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "brand_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandEntity brand;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_customer", columnDefinition = "TEXT")
    private String targetCustomer;

    @Column(name = "key_features", columnDefinition = "TEXT")
    private String keyFeatures;

    @Column(name = "price_range", length = 100)
    private String priceRange;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (type == null) type = "PRODUCT";
    }
}
