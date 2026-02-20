package com.worldcup.hotelbooking.booking.bookingroom;

import java.math.BigDecimal;
import java.time.LocalDateTime;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@Table(
    name = "booking_rooms",
    indexes = {
        @Index(name = "idx_booking_room_booking", columnList = "booking_id"),
        @Index(name = "idx_booking_room_type", columnList = "room_type_id")
    }
)
public class BookingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;


    @Min(1)
    @Column(name = "number_of_rooms", nullable = false)
    private int numberOfRooms;

    @Column(name = "price_per_night_per_room", precision = 10, scale = 2, nullable = false)
    private BigDecimal basePricePerNightPerRoom;

    @Column(name = "total_price_with_fees", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPriceWithFees;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)//FetchType.LAZY means that the associated Booking entity will be loaded on demand, not immediately when the BookingRoom entity is fetched.
    @JoinColumn(name = "booking_id", nullable = false)
    @JsonBackReference
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    @JsonBackReference
    private RoomType roomType;

    

    public BookingRoom() {}

    public BookingRoom(Booking booking, RoomType roomType, int numberOfRooms, BigDecimal pricePerNightPerRoom,BigDecimal totalPriceWithFees) {
        this.booking = booking;
        this.roomType = roomType;
        this.numberOfRooms = numberOfRooms;
        this.basePricePerNightPerRoom = pricePerNightPerRoom;
        this.totalPriceWithFees=totalPriceWithFees;
    }



}

