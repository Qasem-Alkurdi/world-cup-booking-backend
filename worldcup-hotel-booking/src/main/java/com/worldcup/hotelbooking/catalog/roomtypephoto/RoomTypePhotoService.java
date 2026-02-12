package com.worldcup.hotelbooking.catalog.roomtypephoto;

import java.util.List;

public interface RoomTypePhotoService {
    RoomTypePhoto addPhoto(Long roomTypeId, String storageKey, String caption, Integer sortOrder);

    List<RoomTypePhoto> listPhotos(Long roomTypeId);

    void deletePhoto(Long roomTypeId, Long photoId);
}
