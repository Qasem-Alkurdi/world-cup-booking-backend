package com.worldcup.hotelbooking.booking.booking;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.user.AppUser;
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
    @Column(nullable = false)
    private boolean additionalPaymentRequired = false;

    // ── UPDATE SNAPSHOT ──────────────────────────────────────────────────────
    // When a CONFIRMED booking is updated with a higher price, we create a full
    // inactive copy of the original booking (isActive = false) and link it here.
    // If the user pays within 24h the copy is deleted.
    // If they don't, the scheduler restores everything from the copy and deletes it.
    //
    // PENDING bookings do NOT get a copy — the auto-cancel scheduler handles them.
    // CONFIRMED bookings updated with the same price or a lower price do NOT get a
    // copy either — those are applied directly (refund is issued for price decreases).

    // False only on the inactive snapshot copy. Always true on the real booking.
    @Column(nullable = false)
    private boolean active = true;

    // Set on the inactive snapshot copy to point back to the original booking.
    // Null on all normal (active) bookings.
    // We look up the snapshot using bookingReference (the business key) — not id —
    // because bookingReference is the stable, human-readable identifier.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_of_booking_reference", referencedColumnName = "bookingReference")
    private Booking snapshotOf;

    // Deadline by which the user must pay the additional amount after a price-increasing update.
    // Set to now + 24h on the original booking. Cleared when payment completes or booking is reverted.
    private LocalDateTime updatePaymentDeadline;

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