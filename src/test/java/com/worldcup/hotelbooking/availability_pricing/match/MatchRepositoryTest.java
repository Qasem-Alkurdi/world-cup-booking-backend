package com.worldcup.hotelbooking.availability_pricing.match;

import com.worldcup.hotelbooking.security.JwtTokenService;
import com.worldcup.hotelbooking.security.RateLimitService;
import com.worldcup.hotelbooking.tournament.match.Match;
import com.worldcup.hotelbooking.tournament.match.MatchRepository;
import com.worldcup.hotelbooking.tournament.stadium.Stadium;
import com.worldcup.hotelbooking.tournament.stadium.StadiumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)   // use real PostgreSQL
class MatchRepositoryTest {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private StadiumRepository stadiumRepository;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private CacheManager cacheManager;

    private Stadium stadium;
    private Match match1, match2;

    @BeforeEach
    void setUp() {
        stadium = new Stadium();
        stadium.setName("Test Stadium");
        stadium.setCity("Test City");
        stadium = stadiumRepository.save(stadium);

        match1 = new Match();
        match1.setHomeTeam("Team A");
        match1.setAwayTeam("Team B");
        match1.setMatchDateTime(LocalDateTime.now().plusDays(5));
        match1.setStage(Match.MatchStage.GROUP_STAGE_1);
        match1.setStadium(stadium);
        match1 = matchRepository.save(match1);

        match2 = new Match();
        match2.setHomeTeam("Team C");
        match2.setAwayTeam("Team D");
        match2.setMatchDateTime(LocalDateTime.now().plusDays(10));
        match2.setStage(Match.MatchStage.QUARTER_FINAL);
        match2.setStadium(stadium);
        match2 = matchRepository.save(match2);
    }

    @Test
    void existsByStadiumId_shouldReturnTrue() {
        boolean exists = matchRepository.existsByStadiumId(stadium.getId());
        assertThat(exists).isTrue();
    }

    @Test
    void existsByStadiumId_shouldReturnFalse() {
        boolean exists = matchRepository.existsByStadiumId(999L);
        assertThat(exists).isFalse();
    }

    @Test
    void findMatchesBetweenDates_shouldReturnMatchesInRange() {
        LocalDateTime start = LocalDateTime.now().plusDays(4);
        LocalDateTime end = LocalDateTime.now().plusDays(6);
        List<Match> results = matchRepository.findMatchesBetweenDates(start, end);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(match1.getId());
    }

    @Test
    void findMatchesBetweenDates_shouldIgnoreOutside() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(3);
        List<Match> results = matchRepository.findMatchesBetweenDates(start, end);
        assertThat(results).isEmpty();
    }

    @Test
    void findMatchesInCityBetweenDates_shouldReturnMatches() {
        LocalDateTime start = LocalDateTime.now().plusDays(4);
        LocalDateTime end = LocalDateTime.now().plusDays(11);
        List<Match> results = matchRepository.findMatchesInCityBetweenDates("Test City", start, end);
        assertThat(results).hasSize(2);
    }
}