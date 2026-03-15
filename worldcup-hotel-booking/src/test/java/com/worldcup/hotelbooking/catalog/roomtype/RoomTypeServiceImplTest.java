package com.worldcup.hotelbooking.catalog.roomtype;

import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityServiceImpl;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.exception.HotelNotFoundException;
import com.worldcup.hotelbooking.catalog.roomtype.dto.ReplaceRoomTypeRequestDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.RoomTypeAvailabilityCriteria;
import com.worldcup.hotelbooking.catalog.roomtype.exception.RoomTypeAlreadyExistsException;
import com.worldcup.hotelbooking.catalog.roomtype.exception.RoomTypeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.worldcup.hotelbooking.catalog.hotel.HotelStatus.APPROVED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class RoomTypeServiceImplTest {

    private RoomTypeRepository roomTypeRepository;
    private HotelRepository hotelRepository;
    private AvailabilityServiceImpl availabilityService;
    private RoomTypeServiceImpl service;

    @BeforeEach
    void setUp() {
        roomTypeRepository = mock(RoomTypeRepository.class);
        hotelRepository = mock(HotelRepository.class);
        availabilityService = mock(AvailabilityServiceImpl.class);

        service = new RoomTypeServiceImpl(roomTypeRepository, hotelRepository, availabilityService);
    }

    private Hotel buildHotel(Long id) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setStatus(APPROVED);
        hotel.setDeleted(false);
        return hotel;
    }

    private RoomType buildRoomType(Long id, Long hotelId, String name, BigDecimal price) {
        Hotel hotel = buildHotel(hotelId);

        RoomType roomType = new RoomType();
        roomType.setId(id);
        roomType.setHotel(hotel);
        roomType.setName(name);
        roomType.setDescription("Nice room");
        roomType.setMaxAdults(2);
        roomType.setMaxChildren(1);
        roomType.setRoomSizeSqm(new BigDecimal("25.00"));
        roomType.setBasePrice(price);
        roomType.setCurrency("USD");
        roomType.setTotalRooms(10);
        roomType.setCreatedAt(OffsetDateTime.now());
        roomType.setUpdatedAt(OffsetDateTime.now());
        return roomType;
    }

    private ReplaceRoomTypeRequestDto buildReplaceDto(String name) {
        return new ReplaceRoomTypeRequestDto(
                name,
                "Updated description",
                3,
                2,
                new BigDecimal("30.00"),
                new BigDecimal("180.00"),
                "USD",
                8,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true
        );
    }

    @Test
    @DisplayName("findByHotel -> should return room types")
    void findByHotel_ShouldReturnRoomTypes() {
        Hotel hotel = buildHotel(100L);
        List<RoomType> expected = List.of(
                buildRoomType(1L, 100L, "Standard", new BigDecimal("100.00"))
        );

        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(hotel));
        given(roomTypeRepository.findByHotelIdAndHotelNotDeleted(100L))
                .willReturn(expected);

        List<RoomType> result = service.findByHotel(100L);

        assertEquals(1, result.size());
        assertEquals("Standard", result.get(0).getName());
        verify(roomTypeRepository, times(1)).findByHotelIdAndHotelNotDeleted(100L);
    }

    @Test
    @DisplayName("findByHotel -> should throw when hotel not found")
    void findByHotel_WhenHotelNotFound_ShouldThrow() {
        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.empty());

        assertThrows(HotelNotFoundException.class, () -> service.findByHotel(100L));
    }

    @Test
    @DisplayName("findById -> should return room type")
    void findById_ShouldReturnRoomType() {
        Hotel hotel = buildHotel(100L);
        RoomType roomType = buildRoomType(1L, 100L, "Standard", new BigDecimal("100.00"));

        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(hotel));
        given(roomTypeRepository.findByIdAndHotelIdAndHotelNotDeleted(1L, 100L))
                .willReturn(Optional.of(roomType));

        RoomType result = service.findById(100L, 1L);

        assertEquals(1L, result.getId());
        assertEquals("Standard", result.getName());
    }

    @Test
    @DisplayName("findById -> should throw when room type not found")
    void findById_WhenRoomTypeNotFound_ShouldThrow() {
        Hotel hotel = buildHotel(100L);

        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(hotel));
        given(roomTypeRepository.findByIdAndHotelIdAndHotelNotDeleted(1L, 100L))
                .willReturn(Optional.empty());

        assertThrows(RoomTypeNotFoundException.class, () -> service.findById(100L, 1L));
    }

    @Test
    @DisplayName("create -> should save room type")
    void create_ShouldSaveRoomType() {
        Hotel hotel = buildHotel(100L);
        RoomType roomType = buildRoomType(null, 100L, "Standard", new BigDecimal("100.00"));
        RoomType saved = buildRoomType(1L, 100L, "Standard", new BigDecimal("100.00"));

        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(hotel));
        given(roomTypeRepository.existsByHotelIdAndNameIgnoreCaseAndHotelNotDeleted(100L, "Standard"))
                .willReturn(false);
        given(roomTypeRepository.save(roomType)).willReturn(saved);

        RoomType result = service.create(100L, roomType);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(hotel, roomType.getHotel());
        verify(roomTypeRepository, times(1)).save(roomType);
    }

    @Test
    @DisplayName("create -> should throw when duplicate name exists")
    void create_WhenDuplicateName_ShouldThrow() {
        Hotel hotel = buildHotel(100L);
        RoomType roomType = buildRoomType(null, 100L, "Standard", new BigDecimal("100.00"));

        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(hotel));
        given(roomTypeRepository.existsByHotelIdAndNameIgnoreCaseAndHotelNotDeleted(100L, "Standard"))
                .willReturn(true);

        assertThrows(RoomTypeAlreadyExistsException.class, () -> service.create(100L, roomType));
        verify(roomTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("replace -> should update and save room type")
    void replace_ShouldUpdateAndSave() {
        Hotel hotel = buildHotel(100L);
        RoomType current = buildRoomType(1L, 100L, "Standard", new BigDecimal("100.00"));
        ReplaceRoomTypeRequestDto dto = buildReplaceDto("Deluxe");

        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(hotel));
        given(roomTypeRepository.findByIdAndHotelIdAndHotelNotDeleted(1L, 100L))
                .willReturn(Optional.of(current));
        given(roomTypeRepository.existsByHotelIdAndNameIgnoreCaseAndHotelNotDeleted(100L, "Deluxe"))
                .willReturn(false);
        given(roomTypeRepository.save(current)).willReturn(current);

        RoomType result = service.replace(100L, 1L, dto);

        assertEquals("Deluxe", result.getName());
        assertEquals(new BigDecimal("180.00"), result.getBasePrice());
        assertEquals(8, result.getTotalRooms());
        assertTrue(result.isHasCoffeeMachine());
        verify(roomTypeRepository, times(1)).save(current);
    }

    @Test
    @DisplayName("replace -> should throw when new name already exists")
    void replace_WhenDuplicateName_ShouldThrow() {
        Hotel hotel = buildHotel(100L);
        RoomType current = buildRoomType(1L, 100L, "Standard", new BigDecimal("100.00"));
        ReplaceRoomTypeRequestDto dto = buildReplaceDto("Deluxe");

        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(hotel));
        given(roomTypeRepository.findByIdAndHotelIdAndHotelNotDeleted(1L, 100L))
                .willReturn(Optional.of(current));
        given(roomTypeRepository.existsByHotelIdAndNameIgnoreCaseAndHotelNotDeleted(100L, "Deluxe"))
                .willReturn(true);

        assertThrows(RoomTypeAlreadyExistsException.class, () -> service.replace(100L, 1L, dto));
        verify(roomTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete -> should delete room type")
    void delete_ShouldDeleteRoomType() {
        Hotel hotel = buildHotel(100L);
        RoomType current = buildRoomType(1L, 100L, "Standard", new BigDecimal("100.00"));

        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(hotel));
        given(roomTypeRepository.findByIdAndHotelIdAndHotelNotDeleted(1L, 100L))
                .willReturn(Optional.of(current));

        service.delete(100L, 1L);

        verify(roomTypeRepository, times(1)).delete(current);
    }

    @Test
    @DisplayName("findAvailableByHotel -> should return all room types when no criteria")
    void findAvailableByHotel_WhenNoCriteria_ShouldReturnAll() {
        Hotel hotel = buildHotel(100L);
        List<RoomType> roomTypes = List.of(
                buildRoomType(1L, 100L, "Standard", new BigDecimal("100.00")),
                buildRoomType(2L, 100L, "Deluxe", new BigDecimal("150.00"))
        );

        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(hotel));
        given(roomTypeRepository.findByHotelIdAndHotelNotDeleted(100L))
                .willReturn(roomTypes);

        List<RoomType> result = service.findAvailableByHotel(100L, null);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findAvailableByHotel -> should throw when only one date is provided")
    void findAvailableByHotel_WhenOnlyOneDateProvided_ShouldThrow() {
        Hotel hotel = buildHotel(100L);
        RoomTypeAvailabilityCriteria criteria = new RoomTypeAvailabilityCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 3, 20));

        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(hotel));
        given(roomTypeRepository.findByHotelIdAndHotelNotDeleted(100L))
                .willReturn(List.of());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.findAvailableByHotel(100L, criteria)
        );

        assertEquals("checkInDate and checkOutDate must both be provided", ex.getMessage());
    }

    @Test
    @DisplayName("findAvailableByHotel -> should throw when checkout is not after checkin")
    void findAvailableByHotel_WhenInvalidDateRange_ShouldThrow() {
        Hotel hotel = buildHotel(100L);
        RoomTypeAvailabilityCriteria criteria = new RoomTypeAvailabilityCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 3, 20));
        criteria.setCheckOutDate(LocalDate.of(2026, 3, 20));

        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(hotel));
        given(roomTypeRepository.findByHotelIdAndHotelNotDeleted(100L))
                .willReturn(List.of());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.findAvailableByHotel(100L, criteria)
        );

        assertEquals("checkOutDate must be after checkInDate", ex.getMessage());
    }

    @Test
    @DisplayName("findAvailableByHotel -> should filter by capacity and availability and sort by price")
    void findAvailableByHotel_ShouldFilterAndSort() {
        Hotel hotel = buildHotel(100L);

        RoomType standard = buildRoomType(1L, 100L, "Standard", new BigDecimal("120.00"));
        standard.setMaxAdults(2);
        standard.setMaxChildren(1);

        RoomType deluxe = buildRoomType(2L, 100L, "Deluxe", new BigDecimal("90.00"));
        deluxe.setMaxAdults(3);
        deluxe.setMaxChildren(2);

        RoomType small = buildRoomType(3L, 100L, "Small", new BigDecimal("70.00"));
        small.setMaxAdults(1);
        small.setMaxChildren(0);

        RoomTypeAvailabilityCriteria criteria = new RoomTypeAvailabilityCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 3, 20));
        criteria.setCheckOutDate(LocalDate.of(2026, 3, 22));
        criteria.setAdults(2);
        criteria.setChildren(1);
        criteria.setNumberOfRooms(1);

        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(hotel));
        given(roomTypeRepository.findByHotelIdAndHotelNotDeleted(100L))
                .willReturn(List.of(standard, deluxe, small));

        given(availabilityService.checkAvailability(1L, criteria.getCheckInDate(), criteria.getCheckOutDate(), 1))
                .willReturn(true);
        given(availabilityService.checkAvailability(2L, criteria.getCheckInDate(), criteria.getCheckOutDate(), 1))
                .willReturn(true);

        List<RoomType> result = service.findAvailableByHotel(100L, criteria);

        assertEquals(2, result.size());
        assertEquals("Deluxe", result.get(0).getName());
        assertEquals("Standard", result.get(1).getName());
    }
}
