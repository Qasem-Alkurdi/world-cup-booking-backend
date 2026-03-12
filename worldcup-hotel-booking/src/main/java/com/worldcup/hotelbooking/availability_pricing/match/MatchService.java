package com.worldcup.hotelbooking.availability_pricing.match;

import com.worldcup.hotelbooking.availability_pricing.stadium.StadiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final StadiumService stadiumService; // to validate stadium existence

    // Basic CRUD

    @Transactional(readOnly = true)
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Match getMatchById(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new MatchNotFoundException("Match not found with id: " + id));
    }

    public Match createMatch(Match match) {
        // Ensure the referenced stadium exists
        stadiumService.getStadiumById(match.getStadium().getId());
        return matchRepository.save(match);
    }

    public Match updateMatch(Long id, Match updatedMatch) {
        Match existing = getMatchById(id);

        // If stadium changed, verify it exists
        if (!existing.getStadium().getId().equals(updatedMatch.getStadium().getId())) {
            stadiumService.getStadiumById(updatedMatch.getStadium().getId());
        }

        existing.setHomeTeam(updatedMatch.getHomeTeam());
        existing.setAwayTeam(updatedMatch.getAwayTeam());
        existing.setMatchDateTime(updatedMatch.getMatchDateTime());
        existing.setStage(updatedMatch.getStage());
        existing.setStadium(updatedMatch.getStadium());
        existing.setOpeningMatch(updatedMatch.isOpeningMatch());
        existing.setDerby(updatedMatch.isDerby());
        existing.setPopularTeams(updatedMatch.getPopularTeams());

        return matchRepository.save(existing);
    }

    public void deleteMatch(Long id) {
        Match match = getMatchById(id);
        // Optional: check if any bookings reference this match (future)
        matchRepository.delete(match);
    }

    // Additional query methods (used by pricing services)

    @Transactional(readOnly = true)
    public List<Match> getMatchesBetweenDates(LocalDateTime start, LocalDateTime end) {
        return matchRepository.findMatchesBetweenDates(start, end);
    }

    @Transactional(readOnly = true)
    public List<Match> getMatchesInCityBetweenDates(String city, LocalDateTime start, LocalDateTime end) {
        return matchRepository.findMatchesInCityBetweenDates(city, start, end);
    }

    @Transactional(readOnly = true)
    public List<Match> getMatchesByStadium(Long stadiumId) {
        return matchRepository.findByStadiumId(stadiumId);
    }

    @Transactional(readOnly = true)
    public boolean existsByStadiumId(Long stadiumId) {
        return matchRepository.existsByStadiumId(stadiumId);
    }
}