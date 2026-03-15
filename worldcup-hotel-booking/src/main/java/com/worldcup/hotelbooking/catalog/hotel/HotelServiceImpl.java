package com.worldcup.hotelbooking.catalog.hotel;

import com.worldcup.hotelbooking.booking.booking.BookingRepository;
import com.worldcup.hotelbooking.catalog.hotel.dto.UpdateHotelPatchRequest;
import com.worldcup.hotelbooking.catalog.hotel.exception.DeleteConflictException;
import com.worldcup.hotelbooking.catalog.hotel.exception.HotelNotFoundException;
import com.worldcup.hotelbooking.user.user.AppUser;
import com.worldcup.hotelbooking.user.user.AppUserNotFoundException;
import com.worldcup.hotelbooking.user.user.AppUserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static com.worldcup.hotelbooking.booking.booking.Booking.BookingStatus.CONFIRMED;
import static com.worldcup.hotelbooking.booking.booking.Booking.BookingStatus.PENDING;
import static com.worldcup.hotelbooking.catalog.hotel.HotelStatus.APPROVED;

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

    @Transactional(readOnly = true)
    @Override
    public List<Hotel> findAll() {
        return repository.findByStatusAndIsDeletedFalse(APPROVED);
    }

    @Transactional(readOnly = true)
    @Override
    public Hotel findById(Long id) {
        return repository.findByIdAndStatusAndIsDeletedFalse(id, APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(id));
    }

    @Override
    public Hotel create(Hotel hotel, Long ownerId) {
        AppUser owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new AppUserNotFoundException("User not found with id: " + ownerId));

        hotel.setOwner(owner);
        hotel.setStatus(APPROVED);
        hotel.setDeleted(false);

        return repository.save(hotel);
    }

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
                "isDeleted",
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

    @Override
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

    @Override
    public void deleteById(Long id) {
        Hotel hotel = repository.findByIdAndStatusAndIsDeletedFalse(id, APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(id));

        if (bookingRepository.existsByHotel_IdAndStatusIn(id, List.of(PENDING, CONFIRMED))) {
            throw new DeleteConflictException(id);
        }

        hotel.setDeleted(true);
        hotel.setDeletedAt(OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    @Override
    public List<Hotel> getMyHotels(Long ownerId) {
        AppUser owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new AppUserNotFoundException("User not found with id: " + ownerId));

        return repository.findByOwnerAndStatusAndIsDeletedFalse(owner, APPROVED);
    }
}
