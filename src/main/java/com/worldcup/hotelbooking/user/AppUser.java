package com.worldcup.hotelbooking.user;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.worldcup.hotelbooking.reservation.booking.Booking;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String username;

    @NotBlank
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;


    @ElementCollection(targetClass = Role.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private Set<Role> roles = new HashSet<>();


    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExternalProvider> externalProviders = new ArrayList<>();


    private boolean enabled = true;

    private String profilePictureUrl;


    public void addBooking(Booking booking) {
        this.bookings.add(booking);
        booking.setAppUser(this);
    }

    public void removeBooking(Booking booking) {
        this.bookings.remove(booking);
        booking.setAppUser(null);
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }
}
