package com.worldcup.hotelbooking.catalog.hotelphoto;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "hotel_photo",
        indexes = {
                @Index(name = "hotel_photo_hotel_id_idx", columnList = "hotel_id"),
                @Index(name = "hotel_photo_sort_order_idx", columnList = "hotel_id, sort_order"),
                @Index(name = "hotel_photo_storage_key_idx", columnList = "storage_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class HotelPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // matches column: hotel_id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    // matches column: storage_key
    @Column(name = "storage_key", nullable = false, columnDefinition = "TEXT")
    private String storageKey;

    // matches column: caption
    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;

    // matches column: sort_order
    @Column(name = "sort_order")
    private Integer sortOrder;

    // matches column: created_at (timestamptz)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    public HotelPhoto(Hotel hotel, String storageKey, String caption, Integer sortOrder) {
        this.hotel = hotel;
        this.storageKey = storageKey;
        this.caption = caption;
        this.sortOrder = sortOrder;
    }
}
