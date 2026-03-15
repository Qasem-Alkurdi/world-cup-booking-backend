package com.worldcup.hotelbooking.booking.booking;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.user.user.AppUser;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   // @Column(nullable = false, unique = true)
    private String bookingReference;

    private long matchId;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    private int numberOfGuests;
    private int numberOfAdults;
    private int numberOfChildren;


    private LocalDateTime confirmationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;

    private String cancelReason;
    private String cancelledBy;

    // ⭐ NEW FIELDS FOR ADDITIONAL PAYMENT TRACKING
    @Column(nullable = false)
    private BigDecimal totalPrice;
    //private BigDecimal amountPaid;//in case the user confirmed the booking then update it so additional payment will be required so we update the total price and ask fot the additional payment
    @Column(nullable = false)
    private boolean additionalPaymentRequired = false;
   // @Column(precision = 10, scale = 2)
    //private BigDecimal additionalPaymentAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser appUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<BookingRoom> bookingRooms = new ArrayList<>();

    // ---------------- Constructors ----------------

    public Booking() {
    }

    // ---------------- Lifecycle ----------------

    @PrePersist//this method will be called before the entity is persisted to the database. It sets the createdAt timestamp and generates a unique booking reference.
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        bookingReference = generateReference();
    }

    private String generateReference() {
        return "WC2026-" +
                System.currentTimeMillis() + "-" +  // Add timestamp!
                UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }




    public enum BookingStatus {
        PENDING,
        CONFIRMED,
        CHECKED_IN,
        CHECKED_OUT,
        CANCELLED
    }

    // ⭐ HELPER METHOD: Can user check in?
    public boolean canCheckIn() {
        return status == BookingStatus.CONFIRMED &&
                !additionalPaymentRequired;
    }

}
