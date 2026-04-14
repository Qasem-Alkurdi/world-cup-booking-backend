package com.worldcup.hotelbooking.tournament.match;

import com.worldcup.hotelbooking.tournament.stadium.StadiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final StadiumService stadiumService; // to validate stadium existence

    // Basic CRUD

    @Transactional(readOnly = true)
    public Page<Match> getAllMatches(Pageable pageable, String stageParam) {
        if (stageParam == null) {
            return matchRepository.findAll(pageable);
        }

        Match.MatchStage stage;
        try {
            stage = Match.MatchStage.valueOf(stageParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Provide a friendly error with valid options
            String validValues = String.join(", ", Arrays.stream(Match.MatchStage.values())
                    .map(Enum::name)
                    .toList());
            throw new IllegalArgumentException(
                    "Invalid stage value: '" + stageParam + "'. Valid values are: " + validValues
            );
        }
        return matchRepository.findByStage(stage, pageable);
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
    public Page<Match> getMatchesByStadium(Long stadiumId, Pageable pageable) {
        return matchRepository.findByStadiumId(stadiumId, pageable);
    }

    @Transactional(readOnly = true)
    public boolean existsByStadiumId(Long stadiumId) {
        return matchRepository.existsByStadiumId(stadiumId);
    }


    public Page<Match> searchMatches(
            Match.MatchStage stage,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String city,
            Long stadiumId,
            String team,
            Boolean derby,
            Boolean opening,
            Pageable pageable) {

        Specification<Match> spec = Specification
                .where(MatchSpecification.hasStage(stage))
                .and(MatchSpecification.dateBetween(startDate, endDate))
                .and(MatchSpecification.hasCity(city))
                .and(MatchSpecification.hasStadiumId(stadiumId))
                .and(MatchSpecification.teamContains(team))
                .and(MatchSpecification.isDerby(derby))
                .and(MatchSpecification.isOpeningMatch(opening));

        return matchRepository.findAll(spec, pageable);
    }
}