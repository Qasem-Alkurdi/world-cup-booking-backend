package com.worldcup.hotelbooking.catalog.roomtypephoto.mapper;

import com.worldcup.hotelbooking.catalog.roomtypephoto.RoomTypePhoto;
import com.worldcup.hotelbooking.catalog.roomtypephoto.dto.RoomTypePhotoResponseDto;
import com.worldcup.hotelbooking.catalog.storage.PhotoUrlResolver;
import org.springframework.stereotype.Component;

@Component
public class RoomTypePhotoMapper {

    private final PhotoUrlResolver photoUrlResolver;

    public RoomTypePhotoMapper(PhotoUrlResolver photoUrlResolver) {
        this.photoUrlResolver = photoUrlResolver;
    }

    public RoomTypePhotoResponseDto toResponse(RoomTypePhoto photo) {
        return new RoomTypePhotoResponseDto(
                photo.getId(),
                photoUrlResolver.resolve(photo.getStorageKey()),
                photo.getCaption(),
                photo.getSortOrder(),
                photo.getCreatedAt()
        );
    }
}