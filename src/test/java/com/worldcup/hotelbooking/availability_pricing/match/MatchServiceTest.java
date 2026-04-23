package com.worldcup.hotelbooking.availability_pricing.match;

import com.worldcup.hotelbooking.tournament.match.Match;
import com.worldcup.hotelbooking.tournament.match.MatchNotFoundException;
import com.worldcup.hotelbooking.tournament.match.MatchRepository;
import com.worldcup.hotelbooking.tournament.match.MatchService;
import com.worldcup.hotelbooking.tournament.stadium.Stadium;
import com.worldcup.hotelbooking.tournament.stadium.StadiumNotFoundException;
import com.worldcup.hotelbooking.tournament.stadium.StadiumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private StadiumService stadiumService;

    @InjectMocks
    private MatchService matchService;

    private Stadium stadium;
    private Match match;

    @BeforeEach
    void setUp() {
        stadium = new Stadium();
        stadium.setId(1L);
        stadium.setName("Test Stadium");

        match = new Match();
        match.setId(10L);
        match.setHomeTeam("Home");
        match.setAwayTeam("Away");
        match.setStadium(stadium);
    }

    @Test
    void createMatch_validStadium() {
        when(stadiumService.getStadiumById(1L)).thenReturn(stadium);
        when(matchRepository.save(any(Match.class))).thenReturn(match);

        Match created = matchService.createMatch(match);

        assertThat(created).isEqualTo(match);
        verify(stadiumService).getStadiumById(1L);
        verify(matchRepository).save(match);
    }

    @Test
    void createMatch_invalidStadium() {
        when(stadiumService.getStadiumById(1L)).thenThrow(new StadiumNotFoundException("Not found"));

        assertThatThrownBy(() -> matchService.createMatch(match))
                .isInstanceOf(StadiumNotFoundException.class);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void getMatchById_found() {
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        Match found = matchService.getMatchById(10L);
        assertThat(found).isEqualTo(match);
    }

    @Test
    void getMatchById_notFound() {
        when(matchRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> matchService.getMatchById(99L))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void updateMatch_stadiumChanged() {
        Stadium newStadium = new Stadium();
        newStadium.setId(2L);
        Match updatedData = new Match();
        updatedData.setHomeTeam("New Home");
        updatedData.setAwayTeam("New Away");
        updatedData.setStadium(newStadium);

        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(stadiumService.getStadiumById(2L)).thenReturn(newStadium);
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        Match result = matchService.updateMatch(10L, updatedData);

        assertThat(result.getHomeTeam()).isEqualTo("New Home");
        assertThat(result.getStadium()).isEqualTo(newStadium);
        verify(stadiumService).getStadiumById(2L);
    }

    @Test
    void updateMatch_stadiumUnchanged() {
        Match updatedData = new Match();
        updatedData.setHomeTeam("New Home");
        updatedData.setStadium(stadium); // same ID

        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        Match result = matchService.updateMatch(10L, updatedData);

        assertThat(result.getHomeTeam()).isEqualTo("New Home");
        verify(stadiumService, never()).getStadiumById(any());
    }

    @Test
    void deleteMatch_existing() {
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        doNothing().when(matchRepository).delete(match);

        matchService.deleteMatch(10L);

        verify(matchRepository).delete(match);
    }

    @Test
    void deleteMatch_notFound() {
        when(matchRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> matchService.deleteMatch(99L))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void getMatchesBetweenDates() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(5);
        List<Match> expected = List.of(match);
        when(matchRepository.findMatchesBetweenDates(start, end)).thenReturn(expected);

        List<Match> result = matchService.getMatchesBetweenDates(start, end);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void existsByStadiumId() {
        when(matchRepository.existsByStadiumId(1L)).thenReturn(true);
        assertThat(matchService.existsByStadiumId(1L)).isTrue();
    }
}