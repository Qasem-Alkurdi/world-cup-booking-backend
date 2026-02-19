package com.worldcup.hotelbooking.catalog.roomtypephoto;

import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomTypePhotoServiceImpl implements RoomTypePhotoService {

    private final RoomTypePhotoRepository roomTypePhotoRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Override
    public RoomTypePhoto addPhoto(Long roomTypeId, String storageKey, String caption, Integer sortOrder) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new EntityNotFoundException("RoomType not found: " + roomTypeId));

        RoomTypePhoto photo = new RoomTypePhoto(roomType, storageKey, caption, sortOrder);
        return roomTypePhotoRepository.save(photo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypePhoto> listPhotos(Long roomTypeId) {
        return roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId);
    }

    @Override
    public void deletePhoto(Long roomTypeId, Long photoId) {
        if (!roomTypePhotoRepository.existsByRoomTypeIdAndId(roomTypeId, photoId)) {
            throw new EntityNotFoundException("Photo not found for roomTypeId=" + roomTypeId + ", photoId=" + photoId);
        }
        roomTypePhotoRepository.deleteById(photoId);
    }
}
