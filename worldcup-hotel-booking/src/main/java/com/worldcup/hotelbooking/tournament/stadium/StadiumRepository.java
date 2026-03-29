package com.worldcup.hotelbooking.tournament.stadium;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StadiumRepository extends JpaRepository<Stadium, Long> {
    Page<Stadium> findByCityContainingIgnoreCase(String city, Pageable pageable);
}