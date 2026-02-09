package com.worldcup.hotelbooking.catalog.roomtype;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "room_type",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_room_type_name_per_hotel",
                        columnNames = {"hotel_id", "name"}
                )
        },
        indexes = {
                @Index(name = "ix_room_type_hotel_id", columnList = "hotel_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RoomType {

    /* ---------- Identity ---------- */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ---------- Relations ---------- */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    /* ---------- Core fields ---------- */

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Min(1)
    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests;

    @Column(name = "room_size_sqm", precision = 6, scale = 2)
    private BigDecimal roomSizeSqm;

    @NotNull
    @Min(0)
    @Column(name = "base_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal basePrice;

    @NotBlank
    @Column(nullable = false)
    private String currency = "USD";

    @NotNull
    @Min(0)
    @Column(name = "total_rooms", nullable = false)
    private Integer totalRooms;

    /* ---------- Room amenities ---------- */

    @Column(nullable = false)
    private boolean hasPrivateBathroom = false;

    @Column(nullable = false)
    private boolean hasAirConditioning = false;

    @Column(nullable = false)
    private boolean hasHeating = false;

    @Column(nullable = false)
    private boolean hasBalcony = false;

    @Column(nullable = false)
    private boolean hasTv = false;

    @Column(nullable = false)
    private boolean hasMinibar = false;

    @Column(nullable = false)
    private boolean hasSafe = false;

    @Column(nullable = false)
    private boolean hasHairdryer = false;

    @Column(nullable = false)
    private boolean hasWorkDesk = false;

    @Column(nullable = false)
    private boolean hasSoundproofing = false;

    @Column(nullable = false)
    private boolean hasCoffeeMachine = false;

    /* ---------- Audit ---------- */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
