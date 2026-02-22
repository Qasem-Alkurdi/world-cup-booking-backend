package com.worldcup.hotelbooking.catalog.hotel;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.catalog.hotelphoto.HotelPhoto;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.user.user.AppUser;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(
        name = "hotel",
        indexes = {
                @Index(name = "hotel_owner_idx", columnList = "owner_id"),
                @Index(name = "hotel_city_country_idx", columnList = "country, city"),
                @Index(name = "hotel_status_idx", columnList = "status")

        }
)
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;
    @OneToMany(mappedBy = "hotel")
    @JsonManagedReference
    private List<Booking> bookings = new ArrayList<>();
    @OneToMany(mappedBy = "hotel", fetch = FetchType.LAZY)
    @JsonBackReference
    private List<RoomType> roomsType = new ArrayList<>();

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Email
    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @NotBlank
    @Column(nullable = false)
    private String country;

    @NotBlank
    @Column(nullable = false)
    private String city;

    @Column(name = "address_line")
    private String addressLine;

    // PostGIS geography(Point,4326)
    @NotNull
    @Column(columnDefinition = "geography(Point,4326)", nullable = false)
    private Point location;

    // Generated columns (read-only)
    @Column(insertable = false, updatable = false)
    private Double latitude;

    @Column(insertable = false, updatable = false)
    private Double longitude;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    // الأن في الركوستات هتكون كلها APPROVED
    // علشان تطويرها ممكن يأخر الريليز الأولى
    private HotelStatus status;

    @Column(nullable = false)
    private boolean hasWifi = false;

    @Column(nullable = false)
    private boolean hasParking = false;

    @Column(nullable = false)
    private boolean hasBreakfast = false;

    @Column(nullable = false)
    private boolean hasAirConditioning = false;

    @Column(nullable = false)
    private boolean hasHeating = false;

    @Column(nullable = false)
    private boolean hasElevator = false;

    @Column(nullable = false)
    private boolean hasRestaurant = false;

    @Column(nullable = false)
    private boolean hasRoomService = false;

    @Column(nullable = false)
    private boolean hasGym = false;

    @Column(nullable = false)
    private boolean hasPool = false;

    @Column(nullable = false)
    private boolean hasSpa = false;

    @Column(nullable = false)
    private boolean hasLaundry = false;

    @Column(nullable = false)
    private boolean hasAirportShuttle = false;

    @Column(nullable = false)
    private boolean hasAccessibleFacilities = false;

    @Column(nullable = false)
    private boolean petFriendly = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, createdAt ASC")
    @JsonIgnore
    private List<HotelPhoto> photos = new ArrayList<>();

    // --- getters/setters/constructors ---
    public void addBooking(Booking booking) {
        this.bookings.add(booking);
        booking.setHotel(this);
    }

    public void removeBooking(Booking booking) {
        this.bookings.remove(booking);
        booking.setHotel(null);
    }

    public void addPhoto(HotelPhoto photo) {
        this.photos.add(photo);
        photo.setHotel(this);
    }

    public void removePhoto(HotelPhoto photo) {
        this.photos.remove(photo);
        photo.setHotel(null);
    }

    public void addRoomType(RoomType roomType) {
        this.roomsType.add(roomType);
    }

    public void removeRoomType(RoomType roomType) {
        this.roomsType.remove(roomType);
    }

}
