package com.worldcup.hotelbooking.catalog.query.roomtype;

import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingServiceImpl;
import com.worldcup.hotelbooking.catalog.query.roomtype.dto.RoomTypePhotoDto;
import com.worldcup.hotelbooking.catalog.query.roomtype.dto.RoomTypeQueryResponseDto;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeService;
import com.worldcup.hotelbooking.catalog.roomtype.dto.RoomTypeAvailabilityCriteria;
import com.worldcup.hotelbooking.catalog.roomtypephoto.RoomTypePhoto;
import com.worldcup.hotelbooking.catalog.roomtypephoto.RoomTypePhotoRepository;
import com.worldcup.hotelbooking.catalog.storage.PhotoUrlResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only service implementation for catalog room-type queries.
 * Delegates availability/capacity logic to the existing {@link RoomTypeService}
 * and enriches the result with resolved photo URLs.
 */
@Service
@Transactional(readOnly = true)
public class RoomTypeQueryServiceImpl implements RoomTypeQueryService {

    private final RoomTypeService roomTypeService;
    private final RoomTypePhotoRepository roomTypePhotoRepository;
    private final PhotoUrlResolver photoUrlResolver;
    private final EnhancedPricingServiceImpl enhancedPricingService;

    public RoomTypeQueryServiceImpl(
            RoomTypeService roomTypeService,
            RoomTypePhotoRepository roomTypePhotoRepository,
            PhotoUrlResolver photoUrlResolver,
            EnhancedPricingServiceImpl enhancedPricingService
    ) {
        this.roomTypeService = roomTypeService;
        this.roomTypePhotoRepository = roomTypePhotoRepository;
        this.photoUrlResolver = photoUrlResolver;
        this.enhancedPricingService = enhancedPricingService;
    }

    @Override
    public List<RoomTypeQueryResponseDto> findAvailableByHotel(
            Long hotelId,
            RoomTypeAvailabilityCriteria criteria
    ) {
        // 1. Delegate availability + capacity filtering to write-side service
        List<RoomType> roomTypes = roomTypeService.findAvailableByHotel(hotelId, criteria);

        if (roomTypes.isEmpty()) {
            return List.of();
        }

        // 2. Batch load photos for all room types in a single query
        List<Long> roomTypeIds = roomTypes.stream()
                .map(RoomType::getId)
                .toList();

        Map<Long, List<RoomTypePhoto>> photosByRoomType = roomTypePhotoRepository
                .findByRoomTypeIdInOrderBySortOrderAscCreatedAtAsc(roomTypeIds)
                .stream()
                .collect(Collectors.groupingBy(p -> p.getRoomType().getId()));

        // 3. Map to query DTO (with resolved URLs and dynamic pricing)
        return roomTypes.stream()
                .map(rt -> {
                    List<RoomTypePhoto> photos = photosByRoomType.getOrDefault(rt.getId(), List.of());

                    BigDecimal totalPrice = null;
                    BigDecimal nightlyPrice = null;
                    String priceExplanation = null;

                    // Calculate dynamic pricing if dates are provided
                    if (criteria != null && criteria.getCheckInDate() != null && criteria.getCheckOutDate() != null) {
                        int roomsNeeded = criteria.getNumberOfRooms() != null ? criteria.getNumberOfRooms() : 1;

                        totalPrice = enhancedPricingService.calculateTotalStayPrice(
                                criteria.getCheckInDate(),
                                criteria.getCheckOutDate(),
                                rt.getHotel(),
                                rt,
                                roomsNeeded
                        );

                        if (totalPrice != null && totalPrice.compareTo(BigDecimal.ZERO) > 0) {
                            long nights = java.time.temporal.ChronoUnit.DAYS.between(
                                    criteria.getCheckInDate(), criteria.getCheckOutDate());
                            if (nights > 0 && roomsNeeded > 0) {
                                nightlyPrice = totalPrice.divide(
                                        BigDecimal.valueOf(nights * roomsNeeded),
                                        2, java.math.RoundingMode.HALF_UP);
                            }
                            priceExplanation = "Dynamic pricing applied for " + nights + " night"
                                    + (nights > 1 ? "s" : "") + " × " + roomsNeeded + " room"
                                    + (roomsNeeded > 1 ? "s" : "");
                        }
                    }

                    return toQueryDto(rt, photos, totalPrice, nightlyPrice, priceExplanation);
                })
                .toList();
    }

    // ────────────────────────── Mapping ──────────────────────────

    private RoomTypeQueryResponseDto toQueryDto(
            RoomType rt,
            List<RoomTypePhoto> photos,
            BigDecimal totalPrice,
            BigDecimal nightlyPrice,
            String priceExplanation) {

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
