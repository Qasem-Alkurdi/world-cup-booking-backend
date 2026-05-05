package com.worldcup.hotelbooking.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {
    @EntityGraph(attributePaths = {"bookings", "roles"})
    Optional<AppUser> findById(Long id);

    @EntityGraph(attributePaths = {"bookings", "roles"})
    Optional<AppUser> findByEmail(String email);

    @EntityGraph(attributePaths = {"bookings", "roles"})
    Page<AppUser> findAll(Pageable pageable);

    @Override
    Page<AppUser> findAll(Specification<AppUser> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"bookings", "roles"})
    List<AppUser> findAll();

    @EntityGraph(attributePaths = {"bookings", "roles"})
    List<AppUser> findByUsernameContainingIgnoreCase(String username);

    @EntityGraph(attributePaths = {"bookings", "roles"})
    List<AppUser> findByEmailContainingIgnoreCase(String email);

    @EntityGraph(attributePaths = {"bookings", "roles"})
    List<AppUser> findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCase(String username, String email);

    Optional<AppUser> findByUsername(String username);


}