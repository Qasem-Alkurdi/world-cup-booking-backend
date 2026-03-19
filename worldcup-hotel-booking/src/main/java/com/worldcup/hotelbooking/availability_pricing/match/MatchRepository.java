package com.worldcup.hotelbooking.availability_pricing.match;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long>,JpaSpecificationExecutor<Match>  {

    // Basic finder needed for stadium deletion check
    boolean existsByStadiumId(Long stadiumId);

    // Find matches by stadium ID (already covered by exists, but useful)
    Page<Match> findByStadiumId(Long stadiumId, Pageable pageable);

    Page<Match> findByStage(Match.MatchStage stage, Pageable pageable);

    /**
     * Find all matches happening between two dates
     */
    @Query("""
        SELECT m FROM Match m 
        WHERE m.matchDateTime >= :startDate 
        AND m.matchDateTime <= :endDate
        ORDER BY m.matchDateTime
    """)
    List<Match> findMatchesBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find matches near a hotel during a date range, filtered by stadium city
     * (since city is now in Stadium entity)
     */
    @Query("""
        SELECT m FROM Match m 
        JOIN m.stadium s
        WHERE m.matchDateTime >= :startDate 
        AND m.matchDateTime <= :endDate
        AND s.city = :city
        ORDER BY m.stage DESC, m.matchDateTime
    """)
    List<Match> findMatchesInCityBetweenDates(
            @Param("city") String city,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}