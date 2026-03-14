package com.worldcup.hotelbooking.catalog.roomtypephoto;

import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "room_type_photo",
        indexes = {
                @Index(name = "room_type_photo_room_type_id_idx", columnList = "room_type_id"),
                @Index(name = "room_type_photo_sort_order_idx", columnList = "room_type_id, sort_order"),
                @Index(name = "room_type_photo_storage_key_idx", columnList = "storage_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RoomTypePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // matches column: room_type_id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

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

    public RoomTypePhoto(RoomType roomType, String storageKey, String caption, Integer sortOrder) {
        this.roomType = roomType;
        this.storageKey = storageKey;
        this.caption = caption;
        this.sortOrder = sortOrder;
    }
}
