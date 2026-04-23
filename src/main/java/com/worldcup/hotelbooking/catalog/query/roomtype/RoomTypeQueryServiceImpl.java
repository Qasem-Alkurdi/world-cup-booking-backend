package com.worldcup.hotelbooking.catalog.query.roomtype;

import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingServiceImpl;
import com.worldcup.hotelbooking.catalog.query.roomtype.dto.RoomTypeQueryResponseDto;
import com.worldcup.hotelbooking.catalog.query.roomtype.mapper.RoomTypeQueryMapper;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeService;
import com.worldcup.hotelbooking.catalog.roomtype.dto.RoomTypeAvailabilityCriteria;
import com.worldcup.hotelbooking.catalog.roomtypephoto.RoomTypePhoto;
import com.worldcup.hotelbooking.catalog.roomtypephoto.RoomTypePhotoRepository;
import com.worldcup.hotelbooking.reservation.booking.Booking;
import com.worldcup.hotelbooking.tournament.match.Match;
import com.worldcup.hotelbooking.tournament.match.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RoomTypeQueryServiceImpl implements RoomTypeQueryService {

    private final RoomTypeService roomTypeService;
    private final RoomTypePhotoRepository roomTypePhotoRepository;
    private final EnhancedPricingServiceImpl enhancedPricingService;
    private final MatchRepository matchRepository;
    private final RoomTypeQueryMapper mapper;

    public RoomTypeQueryServiceImpl(
            RoomTypeService roomTypeService,
            RoomTypePhotoRepository roomTypePhotoRepository,
            EnhancedPricingServiceImpl enhancedPricingService,
            MatchRepository matchRepository,
            RoomTypeQueryMapper mapper
    ) {
        this.roomTypeService = roomTypeService;
        this.roomTypePhotoRepository = roomTypePhotoRepository;
        this.enhancedPricingService = enhancedPricingService;
        this.matchRepository = matchRepository;
        this.mapper = mapper;
    }

    @Override
    public List<RoomTypeQueryResponseDto> findAvailableByHotel(
            Long hotelId,
            RoomTypeAvailabilityCriteria criteria
    ) {
        // 1. Availability
        List<RoomType> roomTypes = roomTypeService.findAvailableByHotel(hotelId, criteria);

        if (roomTypes.isEmpty()) {
            return List.of();
        }

        // 2. Photos batch loading
        List<Long> roomTypeIds = roomTypes.stream()
                .map(RoomType::getId)
                .toList();

        Map<Long, List<RoomTypePhoto>> photosByRoomType =
                roomTypePhotoRepository
                        .findByRoomTypeIdInOrderBySortOrderAscCreatedAtAsc(roomTypeIds)
                        .stream()
                        .collect(Collectors.groupingBy(p -> p.getRoomType().getId()));

        // 3. Load matches once
        boolean hasDates = criteria != null
                && criteria.getCheckInDate() != null
                && criteria.getCheckOutDate() != null;

        List<Match> matches = List.of();

        if (hasDates) {
            matches = matchRepository.findMatchesBetweenDates(
                    criteria.getCheckInDate().minusDays(1).atStartOfDay(),
                    criteria.getCheckOutDate().plusDays(1).atTime(23, 59, 59)
            );
        }

        // 4. Mapping
        return roomTypes.stream()
                .map(rt -> {

                    List<RoomTypePhoto> photos =
                            photosByRoomType.getOrDefault(rt.getId(), List.of());

                    BigDecimal totalPrice = null;
                    BigDecimal nightlyPrice = null;
                    String priceExplanation = null;

                    if (hasDates) {
                        int roomsNeeded = criteria.getNumberOfRooms() != null
                                ? criteria.getNumberOfRooms()
                                : 1;

                        Booking tempBooking = new Booking();
                        tempBooking.setCheckInDate(criteria.getCheckInDate());
                        tempBooking.setCheckOutDate(criteria.getCheckOutDate());

                        EnhancedPricingServiceImpl.MultiNightPricingBreakdown breakdown =
                                enhancedPricingService.getMultiNightBreakdown(
                                        tempBooking,
                                        rt.getHotel(),
                                        rt,
                                        roomsNeeded
                                );

                        totalPrice = breakdown.getTotalPrice();
                        nightlyPrice = breakdown.getAverageNightlyRate();
                        priceExplanation = breakdown.getExplanation();
                    }

                    return mapper.toDto(
                            rt,
                            photos,
                            totalPrice,
                            nightlyPrice,
                            priceExplanation
                    );
                })
                .toList();
    }
}