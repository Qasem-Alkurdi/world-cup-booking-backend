package com.worldcup.hotelbooking.catalog.hotelphoto.mapper;

import com.worldcup.hotelbooking.catalog.hotelphoto.HotelPhoto;
import com.worldcup.hotelbooking.catalog.hotelphoto.dto.HotelPhotoResponseDto;
import com.worldcup.hotelbooking.catalog.storage.PhotoUrlResolver;
import org.springframework.stereotype.Component;

@Component
public class HotelPhotoMapper {

    private final PhotoUrlResolver photoUrlResolver;

    public HotelPhotoMapper(PhotoUrlResolver photoUrlResolver) {
        this.photoUrlResolver = photoUrlResolver;
    }

    public HotelPhotoResponseDto toResponse(HotelPhoto photo) {
        return new HotelPhotoResponseDto(
                photo.getId(),
                photoUrlResolver.resolve(photo.getStorageKey()),
                photo.getCaption(),
                photo.getSortOrder(),
                photo.getCreatedAt()
        );
    }
}