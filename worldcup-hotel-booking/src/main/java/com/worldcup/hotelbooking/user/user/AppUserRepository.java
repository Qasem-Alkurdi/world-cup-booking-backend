package com.worldcup.hotelbooking.user.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    @EntityGraph(attributePaths = "bookings")
    Optional<AppUser> findById(Long id);
    Optional<AppUser> findByEmail(String email);

    // Add search methods with ignore case for better search
    @EntityGraph(attributePaths = "bookings")
    Page<AppUser> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "bookings")
    List<AppUser> findByUsernameContainingIgnoreCase(String username);

    @EntityGraph(attributePaths = "bookings")
    List<AppUser> findByEmailContainingIgnoreCase(String email);

    @EntityGraph(attributePaths = "bookings")
    List<AppUser> findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCase(String username, String email);


}