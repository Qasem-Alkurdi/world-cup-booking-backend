package com.worldcup.hotelbooking.availability_pricing.stadium;

import com.worldcup.hotelbooking.availability_pricing.match.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StadiumServiceTest {

    @Mock
    private StadiumRepository stadiumRepository;
    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private StadiumService stadiumService;

    private Stadium stadium;

    @BeforeEach
    void setUp() {
        stadium = new Stadium();
        stadium.setId(1L);
        stadium.setName("Test Stadium");
        stadium.setCity("City");
    }

    @Test
    void getStadiumById_found() {
        when(stadiumRepository.findById(1L)).thenReturn(Optional.of(stadium));
        Stadium found = stadiumService.getStadiumById(1L);
        assertThat(found).isEqualTo(stadium);
    }

    @Test
    void getStadiumById_notFound() {
        when(stadiumRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> stadiumService.getStadiumById(99L))
                .isInstanceOf(StadiumNotFoundException.class);
    }

    @Test
    void createStadium() {
        when(stadiumRepository.save(stadium)).thenReturn(stadium);
        Stadium created = stadiumService.createStadium(stadium);
        assertThat(created).isEqualTo(stadium);
    }

    @Test
    void updateStadium() {
        when(stadiumRepository.findById(1L)).thenReturn(Optional.of(stadium));
        when(stadiumRepository.save(any(Stadium.class))).thenAnswer(inv -> inv.getArgument(0));

        Stadium updatedData = new Stadium();
        updatedData.setName("New Name");
        updatedData.setCity("New City");
        updatedData.setCapacity(60000);

        Stadium result = stadiumService.updateStadium(1L, updatedData);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getCity()).isEqualTo("New City");
        assertThat(result.getCapacity()).isEqualTo(60000);
    }

    @Test
    void deleteStadium_noMatches() {
        when(stadiumRepository.findById(1L)).thenReturn(Optional.of(stadium));
        when(matchRepository.existsByStadiumId(1L)).thenReturn(false);

        stadiumService.deleteStadium(1L);

        verify(stadiumRepository).delete(stadium);
    }

    @Test
    void deleteStadium_withMatches_throws() {
        when(stadiumRepository.findById(1L)).thenReturn(Optional.of(stadium));
        when(matchRepository.existsByStadiumId(1L)).thenReturn(true);

        assertThatThrownBy(() -> stadiumService.deleteStadium(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot delete stadium with existing matches");
        verify(stadiumRepository, never()).delete(any());
    }
}