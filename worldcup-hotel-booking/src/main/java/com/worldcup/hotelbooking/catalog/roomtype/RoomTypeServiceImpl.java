package com.worldcup.hotelbooking.catalog.roomtype;

import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityServiceImpl;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.exception.HotelNotFoundException;
import com.worldcup.hotelbooking.catalog.roomtype.dto.ReplaceRoomTypeRequestDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.RoomTypeAvailabilityCriteria;
import com.worldcup.hotelbooking.catalog.roomtype.exception.RoomTypeAlreadyExistsException;
import com.worldcup.hotelbooking.catalog.roomtype.exception.RoomTypeNotFoundException;
import com.worldcup.hotelbooking.catalog.roomtype.mapper.RoomTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.worldcup.hotelbooking.catalog.hotel.HotelStatus.APPROVED;

@Service
@Transactional
public class RoomTypeServiceImpl implements RoomTypeService {
    private final AvailabilityServiceImpl availabilityService;
    private final RoomTypeRepository roomTypeRepository;
    private final HotelRepository hotelRepository;

    public RoomTypeServiceImpl(RoomTypeRepository roomTypeRepository,
                               HotelRepository hotelRepository,
                               AvailabilityServiceImpl availabilityService) {
        this.roomTypeRepository = roomTypeRepository;
        this.hotelRepository = hotelRepository;
        this.availabilityService = availabilityService;
    }

    /* -------------------- Helpers -------------------- */

    private Hotel getApprovedActiveHotel(Long hotelId) {
        return hotelRepository
                .findByIdAndStatusAndIsDeletedFalse(hotelId, APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));
    }

    private RoomType getRoomTypeOrThrow(Long hotelId, Long roomTypeId) {
        // يضمن أن الـ roomType تابع لفندق غير محذوف
        return roomTypeRepository
                .findByIdAndHotelIdAndHotelNotDeleted(roomTypeId, hotelId)
                .orElseThrow(() -> new RoomTypeNotFoundException(hotelId, roomTypeId));
    }

    /* -------------------- CRUD -------------------- */

    /**
     * Returns all room types for a hotel.
     * Cached by hotelId — evicted when any room type under that hotel is created,
     * replaced, or deleted.
     */
    // @Cacheable(value = "roomTypesByHotel", key = "#hotelId")
    @Transactional(readOnly = true)
    @Override
    public List<RoomType> findByHotel(Long hotelId) {
        // يضمن الفندق موجود وغير محذوف + approved
        getApprovedActiveHotel(hotelId);

        return roomTypeRepository.findByHotelIdAndHotelNotDeleted(hotelId);
    }

    /**
     * Returns a single room type.
     * Cached by composite key hotelId + roomTypeId.
     * Evicted when that specific room type is replaced or deleted.
     */
    //@Cacheable(value = "roomTypeById", key = "#hotelId + '_' + #roomTypeId")
    @Transactional(readOnly = true)
    @Override
    public RoomType findById(Long hotelId, Long roomTypeId) {
        // يضمن الفندق موجود وغير محذوف + approved
        getApprovedActiveHotel(hotelId);

        return getRoomTypeOrThrow(hotelId, roomTypeId);
    }

    //    @Caching(evict = {
//            @CacheEvict(value = "hotelById",        key = "#id"),
//            @CacheEvict(value = "hotelList",        allEntries = true),
//            @CacheEvict(value = "myHotels",         allEntries = true),
//            @CacheEvict(value = "hotelPhotos",      key = "#id"),
//            @CacheEvict(value = "roomTypesByHotel", key = "#id"),
//            @CacheEvict(value = "roomTypeById",     allEntries = true),
//            @CacheEvict(value = "roomTypePhotos",   allEntries = true)
//    })
    @Override
    public RoomType create(Long hotelId, RoomType roomType) {
        Hotel hotel = getApprovedActiveHotel(hotelId);

        // name uniqueness per hotel (case-insensitive)
        if (roomTypeRepository.existsByHotelIdAndNameIgnoreCaseAndHotelNotDeleted(hotelId, roomType.getName())) {
            throw new RoomTypeAlreadyExistsException(hotelId, roomType.getName());
        }

        roomType.setId(null);
        roomType.setHotel(hotel);

        return roomTypeRepository.save(roomType);
    }

    //    @Caching(evict = {
//            @CacheEvict(value = "hotelById",        key = "#id"),
//            @CacheEvict(value = "hotelList",        allEntries = true),
//            @CacheEvict(value = "myHotels",         allEntries = true),
//            @CacheEvict(value = "hotelPhotos",      key = "#id"),
//            @CacheEvict(value = "roomTypesByHotel", key = "#id"),
//            @CacheEvict(value = "roomTypeById",     allEntries = true),
//            @CacheEvict(value = "roomTypePhotos",   allEntries = true)
//    })
    @Transactional
    @Override
    public RoomType replace(Long hotelId, Long roomTypeId, ReplaceRoomTypeRequestDto dto) {
        Hotel hotel = getApprovedActiveHotel(hotelId);

        RoomType current = getRoomTypeOrThrow(hotelId, roomTypeId);

        // لو الاسم تغير، افحص uniqueness
        String newName = dto.getName();
        if (newName != null && !newName.equalsIgnoreCase(current.getName())) {
            if (roomTypeRepository.existsByHotelIdAndNameIgnoreCaseAndHotelNotDeleted(hotelId, newName)) {
                throw new RoomTypeAlreadyExistsException(hotelId, newName);
            }
        }

        // replace all updatable fields (explicit, safe)
        RoomTypeMapper.applyReplace(current, dto);

        // تأكيد التابع للفندق (اختياري إذا current أصلاً تابع له، لكنه OK كـ guard)
        current.setHotel(hotel);

        return roomTypeRepository.save(current);
    }


    //    @Caching(evict = {
//            @CacheEvict(value = "hotelById",        key = "#id"),
//            @CacheEvict(value = "hotelList",        allEntries = true),
//            @CacheEvict(value = "myHotels",         allEntries = true),
//            @CacheEvict(value = "hotelPhotos",      key = "#id"),
//            @CacheEvict(value = "roomTypesByHotel", key = "#id"),
//            @CacheEvict(value = "roomTypeById",     allEntries = true),
//            @CacheEvict(value = "roomTypePhotos",   allEntries = true)
//    })
    @Override
    public void delete(Long hotelId, Long roomTypeId) {
        // يضمن الفندق موجود وغير محذوف + approved
        getApprovedActiveHotel(hotelId);

        RoomType current = getRoomTypeOrThrow(hotelId, roomTypeId);
        roomTypeRepository.delete(current);
    }

    @Transactional(readOnly = true)
    @Override
    public List<RoomType> findAvailableByHotel(Long hotelId, RoomTypeAvailabilityCriteria criteria) {
        getApprovedActiveHotel(hotelId);

        List<RoomType> roomTypes = roomTypeRepository.findByHotelIdAndHotelNotDeleted(hotelId);

        if (criteria == null || !hasAvailabilityCriteria(criteria)) {
            return roomTypes;
        }

        validateAvailabilityCriteria(criteria);

        return roomTypes.stream()
                .filter(roomType -> matchesCapacity(roomType, criteria))
                .filter(roomType -> matchesAvailability(roomType, criteria))
                .sorted(java.util.Comparator.comparing(
                        RoomType::getBasePrice,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
                ))
                .toList();
    }

    private boolean hasAvailabilityCriteria(RoomTypeAvailabilityCriteria criteria) {
        // numberOfRooms alone should NOT trigger filtering —
        // it is sent by default from the frontend sidebar.
        // Only dates or capacity (adults/children) should trigger the filter path.
        return criteria.getCheckInDate() != null
                || criteria.getCheckOutDate() != null
                || criteria.getAdults() != null
                || criteria.getChildren() != null;
    }

    private void validateAvailabilityCriteria(RoomTypeAvailabilityCriteria criteria) {
        boolean hasDate = criteria.getCheckInDate() != null || criteria.getCheckOutDate() != null;

        if (hasDate) {
            if (criteria.getCheckInDate() == null || criteria.getCheckOutDate() == null) {
                throw new IllegalArgumentException("checkInDate and checkOutDate must both be provided");
            }

            if (!criteria.getCheckInDate().isBefore(criteria.getCheckOutDate())) {
                throw new IllegalArgumentException("checkOutDate must be after checkInDate");
            }
        }

        if (criteria.getAdults() != null && criteria.getAdults() < 0) {
            throw new IllegalArgumentException("adults must be greater than or equal to 0");
        }

        if (criteria.getChildren() != null && criteria.getChildren() < 0) {
            throw new IllegalArgumentException("children must be greater than or equal to 0");
        }

        if (criteria.getNumberOfRooms() != null && criteria.getNumberOfRooms() <= 0) {
            throw new IllegalArgumentException("numberOfRooms must be greater than 0");
        }
    }

    private boolean matchesCapacity(RoomType roomType, RoomTypeAvailabilityCriteria criteria) {
        int adults = criteria.getAdults() == null ? 0 : criteria.getAdults();
        int children = criteria.getChildren() == null ? 0 : criteria.getChildren();

        return roomType.getMaxAdults() >= adults
                && roomType.getMaxChildren() >= children;
    }

    private boolean matchesAvailability(RoomType roomType, RoomTypeAvailabilityCriteria criteria) {
        if (criteria.getCheckInDate() == null || criteria.getCheckOutDate() == null) {
            return true;
        }

        int requestedRooms = criteria.getNumberOfRooms() == null ? 1 : criteria.getNumberOfRooms();

        return availabilityService.checkAvailability(
                roomType.getId(),
                criteria.getCheckInDate(),
                criteria.getCheckOutDate(),
                requestedRooms
        );
    }
}
