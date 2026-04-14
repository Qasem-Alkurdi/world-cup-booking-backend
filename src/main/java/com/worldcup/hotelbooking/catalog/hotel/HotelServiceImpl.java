package com.worldcup.hotelbooking.catalog.hotel;

import com.worldcup.hotelbooking.booking.booking.BookingRepository;
import com.worldcup.hotelbooking.catalog.hotel.dto.UpdateHotelPatchRequest;
import com.worldcup.hotelbooking.catalog.hotel.exception.DeleteConflictException;
import com.worldcup.hotelbooking.catalog.hotel.exception.HotelNotFoundException;
import com.worldcup.hotelbooking.user.AppUser;
import com.worldcup.hotelbooking.user.AppUserNotFoundException;
import com.worldcup.hotelbooking.user.AppUserRepository;
import com.worldcup.hotelbooking.user.Role;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static com.worldcup.hotelbooking.booking.booking.Booking.BookingStatus.CONFIRMED;
import static com.worldcup.hotelbooking.booking.booking.Booking.BookingStatus.PENDING;
import static com.worldcup.hotelbooking.catalog.hotel.HotelStatus.APPROVED;
import static com.worldcup.hotelbooking.catalog.hotel.HotelStatus.PENDING_APPROVAL;

@Service
@Transactional
public class HotelServiceImpl implements HotelService {

    private final AppUserRepository userRepository;
    private final HotelRepository repository;
    private final BookingRepository bookingRepository;

    public HotelServiceImpl(HotelRepository repository,
                            AppUserRepository userRepository,
                            BookingRepository bookingRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Cache the full approved-hotel list.
     * Evicted whenever any hotel is created, updated, or soft-deleted.
     */
    @Cacheable(value = "hotelList")
    @Transactional(readOnly = true)
    @Override
    public List<Hotel> findAll() {
        return repository.findByStatusAndIsDeletedFalse(APPROVED);
    }

    /**
     * Cache a single hotel by its id.
     * Evicted when that specific hotel is replaced, patched, or deleted.
     */
    @Cacheable(value = "hotelById", key = "#id")
    @Transactional(readOnly = true)
    @Override
    public Hotel findById(Long id) {
        return repository.findByIdAndStatusAndIsDeletedFalse(id, APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(id));
    }

    /**
     * Creating a hotel adds a new entry to the list and to the owner's personal list.
     * hotelById does NOT need eviction — the new hotel has never been cached yet.
     */
    @Caching(evict = {
            @CacheEvict(value = "hotelList", allEntries = true),
            @CacheEvict(value = "myHotels", allEntries = true)
    })
    @Override
    public Hotel create(Hotel hotel, Long ownerId) {
        AppUser owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new AppUserNotFoundException("User not found with id: " + ownerId));

        if (!owner.getRoles().contains(Role.MANAGER)) {
            owner.addRole(Role.MANAGER);
            userRepository.save(owner);
        }

        hotel.setOwner(owner);
        hotel.setStatus(APPROVED);
        hotel.setDeleted(false);
        hotel.setReviewCount(0);
        hotel.setAverageRating(BigDecimal.ZERO);

        return repository.save(hotel);
    }

    /**
     * Full replacement — all hotel fields change.
     * Evict the exact id from hotelById, and blow away the list/owner caches
     * because name, city etc. that appear in list responses may have changed.
     * <p>
     * Cross-service evictions:
     * - roomTypesByHotel / roomTypeById : RoomType entities hold a Hotel reference — if the
     * hotel object changes, any cached RoomType that embeds it becomes stale.
     */

    @Caching(evict = {
            @CacheEvict(value = "hotelById", key = "#id"),
            @CacheEvict(value = "hotelList", allEntries = true),
            @CacheEvict(value = "myHotels", allEntries = true),
            @CacheEvict(value = "roomTypesByHotel", key = "#id"),
            @CacheEvict(value = "roomTypeById", allEntries = true)
    })
    @Override
    public Hotel replace(Long id, Hotel hotel) {
        Hotel current = repository
                .findByIdAndStatusAndIsDeletedFalse(id, APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(id));

        BeanUtils.copyProperties(
                hotel,
                current,
                "id",
                "owner",
                "status",
                "deleted",
                "deletedAt",
                "createdAt",
                "updatedAt",
                "bookings",
                "latitude",
                "longitude"
                , "photos"
        );

        return current;
    }

    /**
     * Partial update — same eviction footprint as replace.
     * Any of the patched fields (name, city, amenities) may appear in cached responses.
     * <p>
     * Cross-service evictions:
     * - roomTypesByHotel / roomTypeById : same reason as replace — RoomType embeds Hotel.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "hotelById", key = "#id"),
            @CacheEvict(value = "hotelList", allEntries = true),
            @CacheEvict(value = "myHotels", allEntries = true),
            @CacheEvict(value = "roomTypesByHotel", key = "#id"),
            @CacheEvict(value = "roomTypeById", allEntries = true)
    })
    public Hotel updatePartial(Long id, UpdateHotelPatchRequest dto) {
        Hotel current = repository
                .findByIdAndStatusAndIsDeletedFalse(id, APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(id));

        if (dto.getName() != null) current.setName(dto.getName());
        if (dto.getDescription() != null) current.setDescription(dto.getDescription());
        if (dto.getContactEmail() != null) current.setContactEmail(dto.getContactEmail());
        if (dto.getContactPhone() != null) current.setContactPhone(dto.getContactPhone());
        if (dto.getAddressLine() != null) current.setAddressLine(dto.getAddressLine());

        if (dto.getHasWifi() != null) current.setHasWifi(dto.getHasWifi());
        if (dto.getHasParking() != null) current.setHasParking(dto.getHasParking());
        if (dto.getHasBreakfast() != null) current.setHasBreakfast(dto.getHasBreakfast());
        if (dto.getHasAirConditioning() != null) current.setHasAirConditioning(dto.getHasAirConditioning());
        if (dto.getHasHeating() != null) current.setHasHeating(dto.getHasHeating());
        if (dto.getHasElevator() != null) current.setHasElevator(dto.getHasElevator());
        if (dto.getHasRestaurant() != null) current.setHasRestaurant(dto.getHasRestaurant());
        if (dto.getHasRoomService() != null) current.setHasRoomService(dto.getHasRoomService());
        if (dto.getHasGym() != null) current.setHasGym(dto.getHasGym());
        if (dto.getHasPool() != null) current.setHasPool(dto.getHasPool());
        if (dto.getHasSpa() != null) current.setHasSpa(dto.getHasSpa());
        if (dto.getHasLaundry() != null) current.setHasLaundry(dto.getHasLaundry());
        if (dto.getHasAirportShuttle() != null) current.setHasAirportShuttle(dto.getHasAirportShuttle());
        if (dto.getHasAccessibleFacilities() != null)
            current.setHasAccessibleFacilities(dto.getHasAccessibleFacilities());
        if (dto.getPetFriendly() != null) current.setPetFriendly(dto.getPetFriendly());

        return current; // no need to save explicitly inside @Transactional
    }

    /**
     * Soft delete — hotel vanishes from all list and single-entity caches.
     * <p>
     * Cross-service evictions:
     * - hotelPhotos      : HotelPhotoServiceImpl caches photos by hotelId; stale after deletion.
     * - roomTypesByHotel : RoomTypeServiceImpl caches room types by hotelId; stale after deletion.
     * - roomTypeById     : individual room type entries all belong to this hotel; all stale.
     * - roomTypePhotos   : RoomTypePhotoServiceImpl caches room type photos; stale after deletion.
     * <p>
     * roomTypeById and roomTypePhotos use allEntries = true because we have only the hotel id here —
     * we cannot cheaply enumerate every roomTypeId under this hotel to evict them by key.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "hotelById", key = "#id"),
            @CacheEvict(value = "hotelList", allEntries = true),
            @CacheEvict(value = "myHotels", allEntries = true),
            @CacheEvict(value = "hotelPhotos", key = "#id"),
            @CacheEvict(value = "roomTypesByHotel", key = "#id"),
            @CacheEvict(value = "roomTypeById", allEntries = true),
            @CacheEvict(value = "roomTypePhotos", allEntries = true)
    })
    public void deleteById(Long id) {
        Hotel hotel = repository.findByIdAndStatusAndIsDeletedFalse(id, APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(id));

        if (bookingRepository.existsByHotel_IdAndStatusIn(id, List.of(PENDING, CONFIRMED))) {
            throw new DeleteConflictException(id);
        }

        hotel.setDeleted(true);
        hotel.setDeletedAt(OffsetDateTime.now());
    }

    /**
     * Cache the owner's hotel list by their user id.
     * Evicted whenever any hotel is created, updated, or deleted
     * (because the owner's list membership or hotel data may have changed).
     */
    @Cacheable(value = "myHotels", key = "#ownerId")
    @Transactional(readOnly = true)
    @Override
    public List<Hotel> getMyHotels(Long ownerId) {
        AppUser owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new AppUserNotFoundException("User not found with id: " + ownerId));

        return repository.findByOwnerAndStatusAndIsDeletedFalse(owner, APPROVED);
    }

    public List<Hotel> GetPENDINGHotels(Long ownerId) {
        AppUser owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new AppUserNotFoundException("User not found with id: " + ownerId));
        return repository.findByOwnerAndStatusAndIsDeletedFalse(owner, PENDING_APPROVAL);
    }
}
