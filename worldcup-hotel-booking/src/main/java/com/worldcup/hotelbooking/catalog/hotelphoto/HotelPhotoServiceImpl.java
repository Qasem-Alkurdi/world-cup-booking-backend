package com.worldcup.hotelbooking.catalog.hotelphoto;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HotelPhotoServiceImpl implements HotelPhotoService {

    private final HotelPhotoRepository hotelPhotoRepository;
    private final HotelRepository hotelRepository;

    @Override
    public HotelPhoto addPhoto(Long hotelId, String storageKey,
                               String caption, Integer sortOrder) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new EntityNotFoundException("Hotel not found: "
                        + hotelId));

        HotelPhoto photo = new HotelPhoto(hotel, storageKey,
                caption, sortOrder);
        return hotelPhotoRepository.save(photo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelPhoto> listPhotos(Long hotelId) {
        return hotelPhotoRepository
                .findByHotelIdOrderBySortOrderAscCreatedAtAsc(hotelId);
    }

    @Override
    public void deletePhoto(Long hotelId, Long photoId) {
        if (!hotelPhotoRepository.existsByHotelIdAndId(hotelId, photoId)) {
            throw new EntityNotFoundException("Photo not found for hotelId="
                    + hotelId + ", photoId=" + photoId);
        }
        hotelPhotoRepository.deleteById(photoId);
    }
}
