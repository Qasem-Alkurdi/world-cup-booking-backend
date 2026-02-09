package com.worldcup.hotelbooking.user.user;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.worldcup.hotelbooking.booking.booking.Booking;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class AppUser {

    @Id @GeneratedValue
    private Long id;
    private String name;

    @OneToMany(mappedBy = "appUser")
    @JsonManagedReference
    private List<Booking> bookings;

    public void addBooking(Booking booking) {
        this.bookings.add(booking);
        booking.setAppUser(this);
    }

    public void removeBooking(Booking booking) {
        this.bookings.remove(booking);
        booking.setAppUser(null);
    }
}
