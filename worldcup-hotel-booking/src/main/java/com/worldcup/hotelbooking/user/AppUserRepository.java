package com.worldcup.hotelbooking.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    @EntityGraph(attributePaths = {"bookings", "roles"})
    Optional<AppUser> findById(Long id);

    Optional<AppUser> findByEmail(String email);

    // Add search methods with ignore case for better search
    @EntityGraph(attributePaths = {"bookings", "roles"})
    Page<AppUser> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "bookings")
    List<AppUser> findByUsernameContainingIgnoreCase(String username);

    @EntityGraph(attributePaths = "bookings")
    List<AppUser> findByEmailContainingIgnoreCase(String email);

    @EntityGraph(attributePaths = "bookings")
    List<AppUser> findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCase(String username, String email);

    Optional<AppUser> findByUsername(String username);


}