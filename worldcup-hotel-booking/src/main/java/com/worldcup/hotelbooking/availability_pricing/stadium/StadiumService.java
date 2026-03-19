package com.worldcup.hotelbooking.availability_pricing.stadium;

import com.worldcup.hotelbooking.availability_pricing.match.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StadiumService {

    private final StadiumRepository stadiumRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public Page<Stadium> getAllStadiums(String city, Pageable pageable) {
        if (city != null && !city.isEmpty()) {
            return stadiumRepository.findByCityContainingIgnoreCase(city, pageable);
        }
        return stadiumRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Stadium getStadiumById(Long id) {
        return stadiumRepository.findById(id)
                .orElseThrow(() -> new StadiumNotFoundException("Stadium not found with id: " + id));
    }

    public Stadium createStadium(Stadium stadium) {
        return stadiumRepository.save(stadium);
    }

    public Stadium updateStadium(Long id, Stadium updatedStadium) {
        Stadium existing = getStadiumById(id);
        existing.setName(updatedStadium.getName());
        existing.setCity(updatedStadium.getCity());
        existing.setLatitude(updatedStadium.getLatitude());
        existing.setLongitude(updatedStadium.getLongitude());
        existing.setCapacity(updatedStadium.getCapacity());
        return stadiumRepository.save(existing);
    }

    public void deleteStadium(Long id) {
        Stadium stadium = getStadiumById(id);
        if (matchRepository.existsByStadiumId(id)) {
            throw new IllegalStateException("Cannot delete stadium with existing matches");
        }
        stadiumRepository.delete(stadium);
    }
}