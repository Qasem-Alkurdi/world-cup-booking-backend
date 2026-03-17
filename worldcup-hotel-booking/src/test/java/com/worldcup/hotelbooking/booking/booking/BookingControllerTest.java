package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.cancellation.CancellationResponseDto;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelService;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeService;
import com.worldcup.hotelbooking.catalog.storage.StaticResourceConfig;
import com.worldcup.hotelbooking.user.user.AppUser;
import com.worldcup.hotelbooking.user.user.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.worldcup.hotelbooking.catalog.storage.StorageProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class,
        excludeAutoConfiguration = {StaticResourceConfig.class})
@Import(BookingControllerTest.TestBeans.class)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerTest {

    @TestConfiguration
    static class TestBeans {
        @Bean
        StorageProperties storageProperties() {
            return new StorageProperties();
        }
    }
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingServiceImpl bookingService;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private HotelService hotelService;

    @MockitoBean
    private RoomTypeService roomTypeService;

    private Booking booking;
    private BookingRoom bookingRoom;
    private AppUser user;
    private Hotel hotel;
    private RoomType roomType;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(1L);
        user.setUsername("test-user");

        hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Test Hotel");

        roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("Deluxe Room");
        roomType.setBasePrice(BigDecimal.valueOf(120));
        roomType.setHotel(hotel);

        bookingRoom = new BookingRoom();
        bookingRoom.setId(1L);
        bookingRoom.setRoomType(roomType);
        bookingRoom.setNumberOfRooms(1);
        bookingRoom.setBasePricePerNightPerRoom(BigDecimal.valueOf(120));
        bookingRoom.setTotalPriceWithFees(BigDecimal.valueOf(240));

        booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("WC2026-REF-001");
        booking.setAppUser(user);
        booking.setHotel(hotel);
        booking.setCheckInDate(LocalDate.of(2026, 7, 1));
        booking.setCheckOutDate(LocalDate.of(2026, 7, 5));
        booking.setNumberOfGuests(4);
        booking.setNumberOfAdults(2);
        booking.setNumberOfChildren(2);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setConfirmationDeadline(LocalDateTime.of(2026, 6, 28, 10, 0));
        booking.setTotalPrice(BigDecimal.valueOf(480));
        booking.setAdditionalPaymentRequired(false);
        booking.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));
        bookingRoom.setBooking(booking);
    }

    @Test
    void getBookingById_shouldReturnBookingById() throws Exception {
        // Arrange
        when(bookingService.getBookingById(1L)).thenReturn(booking);

        // Act + Assert
        mockMvc.perform(get("/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookingReference").value("WC2026-REF-001"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.numberOfGuests").value(4));

        verify(bookingService).getBookingById(1L);
    }

    @Test
    void createBooking_shouldCreateBookingSuccessfully() throws Exception {
        // Arrange
        when(appUserService.getUserById(1L)).thenReturn(user);
        when(hotelService.findById(1L)).thenReturn(hotel);
        when(roomTypeService.findById(1L, 1L)).thenReturn(roomType);
        when(bookingService.createBooking(any(Booking.class))).thenReturn(booking);

        String requestBody = """
                {
                  "hotelId": 1,
                  "userId": 1,
                  "checkInDate": "2026-07-01",
                  "checkOutDate": "2026-07-05",
                  "numberOfGuests": 4,
                  "numberOfAdults": 2,
                  "numberOfChildren": 2,
                  "rooms": [
                    {
                      "roomTypeId": 1,
                      "numberOfRooms": 1
                    }
                  ]
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/bookings/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookingReference").value("WC2026-REF-001"))
                .andExpect(jsonPath("$.rooms[0].roomTypeName").value("Deluxe Room"));

        verify(appUserService).getUserById(1L);
        verify(hotelService).findById(1L);
        verify(roomTypeService).findById(1L, 1L);
        verify(bookingService).createBooking(any(Booking.class));
    }

    @Test
    void getCancellationPolicy_shouldReturnPreview() throws Exception {
        // Arrange
        CancellationResponseDto response = CancellationResponseDto.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(300))
                .cancellationFee(BigDecimal.valueOf(20))
                .refundPercentage(75)
                .policyMessage("75% refund policy")
                .daysUntilCheckIn(20)
                .build();

        when(bookingService.previewCancellation(1L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/bookings/1/cancellation-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canCancel").value(true))
                .andExpect(jsonPath("$.refundAmount").value(300))
                .andExpect(jsonPath("$.cancellationFee").value(20))
                .andExpect(jsonPath("$.refundPercentage").value(75))
                .andExpect(jsonPath("$.policyMessage").value("75% refund policy"));

        verify(bookingService).previewCancellation(1L);
    }

    @Test
    void cancelBooking_shouldCancelBookingSuccessfully() throws Exception {
        // Arrange
        CancellationResponseDto policy = CancellationResponseDto.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(240))
                .cancellationFee(BigDecimal.ZERO)
                .refundPercentage(100)
                .policyMessage("Full refund")
                .daysUntilCheckIn(30)
                .build();

        Booking cancelled = new Booking();
        cancelled.setId(1L);
        cancelled.setBookingReference("WC2026-REF-001");
        cancelled.setStatus(Booking.BookingStatus.CANCELLED);
        cancelled.setCheckInDate(booking.getCheckInDate());
        cancelled.setCheckOutDate(booking.getCheckOutDate());
        cancelled.setNumberOfGuests(4);
        cancelled.setNumberOfAdults(2);
        cancelled.setNumberOfChildren(2);
        cancelled.setTotalPrice(BigDecimal.valueOf(480));
        cancelled.setConfirmationDeadline(booking.getConfirmationDeadline());
        cancelled.setAdditionalPaymentRequired(false);
        cancelled.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));

        when(bookingService.previewCancellation(1L)).thenReturn(policy);
        when(bookingService.cancelBooking(1L, "Travel plans changed")).thenReturn(cancelled);

        // Act + Assert
        mockMvc.perform(put("/bookings/1/cancel")
                        .param("reason", "Travel plans changed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booking.id").value(1))
                .andExpect(jsonPath("$.booking.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(240))
                .andExpect(jsonPath("$.refundPercentage").value(100))
                .andExpect(jsonPath("$.policyApplied").value("Full refund"));

        verify(bookingService).previewCancellation(1L);
        verify(bookingService).cancelBooking(1L, "Travel plans changed");
    }

    @Test
    void cancelBooking_shouldReturnBadRequest_whenReasonIsInvalid() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(put("/bookings/1/cancel")
                        .param("reason", "   "))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).previewCancellation(anyLong());
        verify(bookingService, never()).cancelBooking(anyLong(), anyString());
    }

    @Test
    void getBookingRooms_shouldReturnBookingRooms() throws Exception {
        // Arrange
        when(bookingService.getBookingById(1L)).thenReturn(booking);

        // Act + Assert
        mockMvc.perform(get("/bookings/1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomTypeName").value("Deluxe Room"))
                .andExpect(jsonPath("$[0].numberOfRooms").value(1))
                .andExpect(jsonPath("$[0].basePricePerNight").value(120))
                .andExpect(jsonPath("$[0].totalPriceWithFees").value(240));

        verify(bookingService).getBookingById(1L);
    }

    @Test
    void getBookingByReference_shouldReturnBooking() throws Exception {
        // Arrange
        when(bookingService.findBookingByReference("WC2026-REF-001")).thenReturn(booking);

        // Act + Assert
        mockMvc.perform(get("/bookings/reference/WC2026-REF-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookingReference").value("WC2026-REF-001"));

        verify(bookingService).findBookingByReference("WC2026-REF-001");
    }

    @Test
    void updateBooking_shouldUpdateBookingSuccessfully() throws Exception {
        // Arrange
        when(appUserService.getUserById(1L)).thenReturn(user);
        when(hotelService.findById(1L)).thenReturn(hotel);
        when(roomTypeService.findById(1L, 1L)).thenReturn(roomType);
        when(bookingService.updateExisting(eq(1L), any(Booking.class))).thenReturn(booking);

        String requestBody = """
                {
                  "hotelId": 1,
                  "userId": 1,
                  "checkInDate": "2026-07-02",
                  "checkOutDate": "2026-07-06",
                  "numberOfGuests": 4,
                  "numberOfAdults": 2,
                  "numberOfChildren": 2,
                  "rooms": [
                    {
                      "roomTypeId": 1,
                      "numberOfRooms": 1
                    }
                  ]
                }
                """;

        // Act + Assert
        mockMvc.perform(put("/bookings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookingReference").value("WC2026-REF-001"));

        verify(appUserService).getUserById(1L);
        verify(hotelService).findById(1L);
        verify(roomTypeService).findById(1L, 1L);
        verify(bookingService).updateExisting(eq(1L), any(Booking.class));
    }

    @Test
    void getMyHistory_shouldReturnPaginatedBookingHistory() throws Exception {
        // Arrange
        Page<Booking> page = new PageImpl<>(List.of(booking), PageRequest.of(0, 10), 1);
        when(bookingService.getGuestHistory(eq(1L), any())).thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/bookings/my/history")
                        .param("userId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].bookingReference").value("WC2026-REF-001"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(bookingService).getGuestHistory(eq(1L), any());
    }

    @Test
    void getUpcoming_shouldReturnUpcomingBookings() throws Exception {
        // Arrange
        Page<Booking> page = new PageImpl<>(List.of(booking), PageRequest.of(0, 10), 1);
        when(bookingService.getHotelUpcomingBookings(eq(1L), any())).thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/bookings/hotel/1/upcoming")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(bookingService).getHotelUpcomingBookings(eq(1L), any());
    }

    @Test
    void filterBookings_shouldReturnFilteredBookings() throws Exception {
        // Arrange
        Page<Booking> page = new PageImpl<>(List.of(booking), PageRequest.of(0, 10), 1);
        when(bookingService.filterBookings(eq(1L), eq(1L), eq(Booking.BookingStatus.CONFIRMED), any(LocalDate.class), any(LocalDate.class), eq(100.0), eq(500.0), any()))
                .thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/bookings")
                        .param("userId", "1")
                        .param("hotelId", "1")
                        .param("status", "CONFIRMED")
                        .param("fromDate", "2026-07-01")
                        .param("toDate", "2026-07-10")
                        .param("minPrice", "100")
                        .param("maxPrice", "500")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].bookingReference").value("WC2026-REF-001"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(bookingService).filterBookings(eq(1L), eq(1L), eq(Booking.BookingStatus.CONFIRMED), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 10)), eq(100.0), eq(500.0), any());
    }

    @Test
    void checkInBooking_shouldCheckInSuccessfully() throws Exception {
        // Arrange
        Booking checkedIn = new Booking();
        checkedIn.setId(1L);
        checkedIn.setBookingReference("WC2026-REF-001");
        checkedIn.setStatus(Booking.BookingStatus.CHECKED_IN);
        checkedIn.setCheckInDate(booking.getCheckInDate());
        checkedIn.setCheckOutDate(booking.getCheckOutDate());
        checkedIn.setNumberOfGuests(4);
        checkedIn.setNumberOfAdults(2);
        checkedIn.setNumberOfChildren(2);
        checkedIn.setTotalPrice(BigDecimal.valueOf(480));
        checkedIn.setConfirmationDeadline(booking.getConfirmationDeadline());
        checkedIn.setAdditionalPaymentRequired(false);
        checkedIn.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));

        when(bookingService.checkInBooking(1L)).thenReturn(checkedIn);

        // Act + Assert
        mockMvc.perform(put("/bookings/1/checkin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));

        verify(bookingService).checkInBooking(1L);
    }

    @Test
    void checkOutBooking_shouldCheckOutSuccessfully() throws Exception {
        // Arrange
        Booking checkedOut = new Booking();
        checkedOut.setId(1L);
        checkedOut.setBookingReference("WC2026-REF-001");
        checkedOut.setStatus(Booking.BookingStatus.CHECKED_OUT);
        checkedOut.setCheckInDate(booking.getCheckInDate());
        checkedOut.setCheckOutDate(booking.getCheckOutDate());
        checkedOut.setNumberOfGuests(4);
        checkedOut.setNumberOfAdults(2);
        checkedOut.setNumberOfChildren(2);
        checkedOut.setTotalPrice(BigDecimal.valueOf(480));
        checkedOut.setConfirmationDeadline(booking.getConfirmationDeadline());
        checkedOut.setAdditionalPaymentRequired(false);
        checkedOut.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));

        when(bookingService.checkOutBooking(1L)).thenReturn(checkedOut);

        // Act + Assert
        mockMvc.perform(put("/bookings/1/checkout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"));

        verify(bookingService).checkOutBooking(1L);
    }
}
