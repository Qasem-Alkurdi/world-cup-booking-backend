package com.worldcup.hotelbooking.catalog.hotel;

import com.worldcup.hotelbooking.booking.booking.BookingRepository;
import com.worldcup.hotelbooking.catalog.hotel.dto.UpdateHotelPatchRequest;
import com.worldcup.hotelbooking.catalog.hotel.exception.DeleteConflictException;
import com.worldcup.hotelbooking.catalog.hotel.exception.HotelNotFoundException;
import com.worldcup.hotelbooking.user.AppUser;
import com.worldcup.hotelbooking.user.AppUserNotFoundException;
import com.worldcup.hotelbooking.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.worldcup.hotelbooking.booking.booking.Booking.BookingStatus.CONFIRMED;
import static com.worldcup.hotelbooking.booking.booking.Booking.BookingStatus.PENDING;
import static com.worldcup.hotelbooking.catalog.hotel.HotelStatus.APPROVED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hotel Service Impl Tests")
class HotelServiceImplTest {

    @Mock
    private HotelRepository repository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private HotelServiceImpl hotelService;

    private Hotel testHotel;
    private AppUser testOwner;

    @BeforeEach
    void setUp() {
        testOwner = new AppUser();
        testOwner.setId(10L);
        testOwner.setUsername("owner");

        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("Test Hotel");
        testHotel.setDescription("Nice place");
        testHotel.setStatus(APPROVED);
        testHotel.setDeleted(false);
        testHotel.setOwner(testOwner);
    }

    @Test
    @DisplayName("findAll should return approved and not deleted hotels")
    void findAll_ReturnsApprovedHotels() {
        when(repository.findByStatusAndIsDeletedFalse(APPROVED)).thenReturn(List.of(testHotel));

        List<Hotel> result = hotelService.findAll();

        assertEquals(1, result.size());
        assertEquals(testHotel, result.get(0));
        verify(repository).findByStatusAndIsDeletedFalse(APPROVED);
    }

    @Test
    @DisplayName("findById should return hotel when exists")
    void findById_ExistingHotel_ReturnsHotel() {
        when(repository.findByIdAndStatusAndIsDeletedFalse(1L, APPROVED))
                .thenReturn(Optional.of(testHotel));

        Hotel result = hotelService.findById(1L);

        assertEquals(testHotel, result);
        verify(repository).findByIdAndStatusAndIsDeletedFalse(1L, APPROVED);
    }

    @Test
    @DisplayName("findById should throw when hotel does not exist")
    void findById_NotFound_ThrowsHotelNotFoundException() {
        when(repository.findByIdAndStatusAndIsDeletedFalse(99L, APPROVED))
                .thenReturn(Optional.empty());

        assertThrows(HotelNotFoundException.class, () -> hotelService.findById(99L));
        verify(repository).findByIdAndStatusAndIsDeletedFalse(99L, APPROVED);
    }

    @Test
    @DisplayName("create should set owner status deleted flag and save")
    void create_ValidOwner_SetsFieldsAndSaves() {
        Hotel toCreate = new Hotel();
        toCreate.setName("Created Hotel");

        when(userRepository.findById(10L)).thenReturn(Optional.of(testOwner));
        when(repository.save(any(Hotel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Hotel result = hotelService.create(toCreate, 10L);

        assertEquals(testOwner, result.getOwner());
        assertEquals(APPROVED, result.getStatus());
        assertFalse(result.isDeleted());
        verify(userRepository).findById(10L);
        verify(repository).save(toCreate);
    }

    @Test
    @DisplayName("create should throw when owner does not exist")
    void create_OwnerNotFound_ThrowsAppUserNotFoundException() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(AppUserNotFoundException.class, () -> hotelService.create(new Hotel(), 404L));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("replace should update mutable fields and keep protected fields")
    void replace_ExistingHotel_UpdatesAllowedFieldsOnly() {
        Hotel existing = new Hotel();
        existing.setId(1L);
        existing.setOwner(testOwner);
        existing.setStatus(APPROVED);
        existing.setDeleted(false);
        existing.setLatitude(24.0);
        existing.setLongitude(46.0);
        existing.setName("Old Name");
        existing.setDescription("Old Desc");

        Hotel incoming = new Hotel();
        incoming.setId(200L);
        incoming.setOwner(new AppUser());
        incoming.setStatus(HotelStatus.PENDING_APPROVAL);
        incoming.setDeleted(true);
        incoming.setLatitude(1.1);
        incoming.setLongitude(2.2);
        incoming.setName("New Name");
        incoming.setDescription("New Desc");

        when(repository.findByIdAndStatusAndIsDeletedFalse(1L, APPROVED))
                .thenReturn(Optional.of(existing));

        Hotel result = hotelService.replace(1L, incoming);

        assertEquals("New Name", result.getName());
        assertEquals("New Desc", result.getDescription());
        assertEquals(1L, result.getId());
        assertEquals(testOwner, result.getOwner());
        assertEquals(APPROVED, result.getStatus());
        assertFalse(result.isDeleted());
        assertEquals(24.0, result.getLatitude());
        assertEquals(46.0, result.getLongitude());
    }

    @Test
    @DisplayName("replace should throw when hotel does not exist")
    void replace_NotFound_ThrowsHotelNotFoundException() {
        when(repository.findByIdAndStatusAndIsDeletedFalse(2L, APPROVED))
                .thenReturn(Optional.empty());

        assertThrows(HotelNotFoundException.class, () -> hotelService.replace(2L, new Hotel()));
    }

    @Test
    @DisplayName("updatePartial should update only non-null fields")
    void updatePartial_UpdatesOnlyProvidedFields() {
        Hotel existing = new Hotel();
        existing.setId(1L);
        existing.setName("Old");
        existing.setDescription("Desc");
        existing.setHasWifi(false);
        existing.setHasPool(false);
        existing.setAddressLine("Addr");

        UpdateHotelPatchRequest dto = new UpdateHotelPatchRequest();
        dto.setName("New");
        dto.setHasWifi(true);
        dto.setHasPool(true);
        dto.setAddressLine("New Addr");

        when(repository.findByIdAndStatusAndIsDeletedFalse(1L, APPROVED))
                .thenReturn(Optional.of(existing));

        Hotel result = hotelService.updatePartial(1L, dto);

        assertEquals("New", result.getName());
        assertEquals("Desc", result.getDescription());
        assertTrue(result.isHasWifi());
        assertTrue(result.isHasPool());
        assertEquals("New Addr", result.getAddressLine());
    }

    @Test
    @DisplayName("updatePartial should throw when hotel does not exist")
    void updatePartial_NotFound_ThrowsHotelNotFoundException() {
        when(repository.findByIdAndStatusAndIsDeletedFalse(7L, APPROVED))
                .thenReturn(Optional.empty());

        assertThrows(HotelNotFoundException.class,
                () -> hotelService.updatePartial(7L, new UpdateHotelPatchRequest()));
    }

    @Test
    @DisplayName("deleteById should soft delete when no active bookings")
    void deleteById_NoActiveBookings_SoftDeletesHotel() {
        Hotel existing = new Hotel();
        existing.setId(1L);
        existing.setDeleted(false);
        existing.setStatus(APPROVED);

        when(repository.findByIdAndStatusAndIsDeletedFalse(1L, APPROVED))
                .thenReturn(Optional.of(existing));
        when(bookingRepository.existsByHotel_IdAndStatusIn(1L, List.of(PENDING, CONFIRMED)))
                .thenReturn(false);

        hotelService.deleteById(1L);

        assertTrue(existing.isDeleted());
        assertNotNull(existing.getDeletedAt());
    }

    @Test
    @DisplayName("deleteById should throw conflict when active bookings exist")
    void deleteById_ActiveBookings_ThrowsDeleteConflictException() {
        when(repository.findByIdAndStatusAndIsDeletedFalse(1L, APPROVED))
                .thenReturn(Optional.of(testHotel));
        when(bookingRepository.existsByHotel_IdAndStatusIn(1L, List.of(PENDING, CONFIRMED)))
                .thenReturn(true);

        assertThrows(DeleteConflictException.class, () -> hotelService.deleteById(1L));
    }

    @Test
    @DisplayName("deleteById should throw when hotel does not exist")
    void deleteById_NotFound_ThrowsHotelNotFoundException() {
        when(repository.findByIdAndStatusAndIsDeletedFalse(500L, APPROVED))
                .thenReturn(Optional.empty());

        assertThrows(HotelNotFoundException.class, () -> hotelService.deleteById(500L));
        verify(bookingRepository, never()).existsByHotel_IdAndStatusIn(any(), any());
    }

    @Test
    @DisplayName("getMyHotels should return owner hotels")
    void getMyHotels_ValidOwner_ReturnsHotels() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testOwner));
        when(repository.findByOwnerAndStatusAndIsDeletedFalse(testOwner, APPROVED))
                .thenReturn(List.of(testHotel));

        List<Hotel> result = hotelService.getMyHotels(10L);

        assertEquals(1, result.size());
        assertEquals(testHotel, result.get(0));
        verify(repository).findByOwnerAndStatusAndIsDeletedFalse(testOwner, APPROVED);
    }

    @Test
    @DisplayName("getMyHotels should throw when owner not found")
    void getMyHotels_OwnerNotFound_ThrowsAppUserNotFoundException() {
        when(userRepository.findById(77L)).thenReturn(Optional.empty());

        assertThrows(AppUserNotFoundException.class, () -> hotelService.getMyHotels(77L));
        verify(repository, never()).findByOwnerAndStatusAndIsDeletedFalse(any(), any());
    }
}
