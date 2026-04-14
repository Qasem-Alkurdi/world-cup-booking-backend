package com.worldcup.hotelbooking.catalog.query.roomtype.mapper;

import com.worldcup.hotelbooking.catalog.query.roomtype.dto.RoomTypePhotoDto;
import com.worldcup.hotelbooking.catalog.query.roomtype.dto.RoomTypeQueryResponseDto;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtypephoto.RoomTypePhoto;
import com.worldcup.hotelbooking.catalog.storage.PhotoUrlResolver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RoomTypeQueryMapper {

    private final PhotoUrlResolver photoUrlResolver;

    public RoomTypeQueryMapper(PhotoUrlResolver photoUrlResolver) {
        this.photoUrlResolver = photoUrlResolver;
    }

    public RoomTypeQueryResponseDto toDto(
            RoomType rt,
            List<RoomTypePhoto> photos,
            BigDecimal totalPrice,
            BigDecimal nightlyPrice,
            String priceExplanation
    ) {

        List<RoomTypePhotoDto> photoDtos = photos.stream()
                .map(p -> new RoomTypePhotoDto(
                        p.getId(),
                        photoUrlResolver.resolve(p.getStorageKey()),
                        p.getCaption(),
                        p.isPrimary(),
                        p.getSortOrder()
                ))
                .toList();

        String primaryPhotoUrl = photoDtos.stream()
                .filter(RoomTypePhotoDto::primary)
                .map(RoomTypePhotoDto::url)
                .findFirst()
                .orElseGet(() -> photoDtos.isEmpty() ? null : photoDtos.get(0).url());

        return new RoomTypeQueryResponseDto(
                rt.getId(),
                rt.getHotel().getId(),

                rt.getName(),
                rt.getDescription(),

                rt.getMaxAdults(),
                rt.getMaxChildren(),

                rt.getRoomSizeSqm(),
                rt.getBasePrice(),
                rt.getCurrency(),

                totalPrice,
                nightlyPrice,
                priceExplanation,

                rt.getTotalRooms(),

                rt.isHasPrivateBathroom(),
                rt.isHasAirConditioning(),
                rt.isHasHeating(),
                rt.isHasBalcony(),
                rt.isHasTv(),
                rt.isHasMinibar(),
                rt.isHasSafe(),
                rt.isHasHairdryer(),
                rt.isHasWorkDesk(),
                rt.isHasSoundproofing(),
                rt.isHasCoffeeMachine(),

                primaryPhotoUrl,
                photoDtos
        );
    }
}