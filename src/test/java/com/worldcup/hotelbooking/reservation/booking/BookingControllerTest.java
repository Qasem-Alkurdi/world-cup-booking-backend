package com.worldcup.hotelbooking.reservation.booking;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelService;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeService;
import com.worldcup.hotelbooking.catalog.storage.StaticResourceConfig;
import com.worldcup.hotelbooking.reservation.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.reservation.cancellation.CancellationResponse;
import com.worldcup.hotelbooking.security.JwtTokenService;
import com.worldcup.hotelbooking.security.RateLimitService;
import com.worldcup.hotelbooking.user.AppUser;
import com.worldcup.hotelbooking.user.AppUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = BookingController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = StaticResourceConfig.class
        ),
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerTest {

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

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private CacheManager cacheManager;

    private Booking buildBooking(Long bookingId, Long userId, Long hotelId) {
        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("guest-user");

        Hotel hotel = new Hotel();
        hotel.setId(hotelId);
        hotel.setName("Royal Hotel");

        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("Deluxe Room");
        roomType.setBasePrice(BigDecimal.valueOf(120));
        roomType.setHotel(hotel);

        Booking booking = new Booking();
        booking.setId(bookingId);
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

        BookingRoom bookingRoom = new BookingRoom();
        bookingRoom.setId(1L);
        bookingRoom.setRoomType(roomType);
        bookingRoom.setNumberOfRooms(1);
        bookingRoom.setBasePricePerNightPerRoom(BigDecimal.valueOf(120));
        bookingRoom.setTotalPriceWithFees(BigDecimal.valueOf(240));
        bookingRoom.setBooking(booking);

        booking.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));

        return booking;
    }

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("GET /bookings/{id} -> should return booking by id")
    void getBookingById_ShouldReturnBooking() throws Exception {
        Booking booking = buildBooking(1L, 10L, 1L);

        given(bookingService.getBookingById(1L)).willReturn(booking);

        mockMvc.perform(get("/bookings/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookingReference").value("WC2026-REF-001"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.numberOfGuests").value(4))
                .andExpect(jsonPath("$.totalPrice").value(480));

        verify(bookingService, times(1)).getBookingById(1L);
    }

    @Test
    @DisplayName("POST /bookings -> should create booking and return 201")
    void createBooking_ShouldReturnCreatedBooking() throws Exception {
        AppUser user = new AppUser();
        user.setId(10L);

        Hotel hotel = new Hotel();
        hotel.setId(1L);

        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setHotel(hotel);

        Booking saved = buildBooking(1L, 10L, 1L);

        given(appUserService.getUserById(10L)).willReturn(user);
        given(hotelService.findById(1L)).willReturn(hotel);
        given(roomTypeService.findById(1L, 1L)).willReturn(roomType);
        given(bookingService.createBooking(any(Booking.class))).willReturn(saved);

        String requestBody = """
                {
                  "hotelId": 1,
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

        mockMvc.perform(post("/bookings")
                        .principal(jwtAuthenticationToken(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/bookings/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookingReference").value("WC2026-REF-001"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(appUserService, times(1)).getUserById(10L);
        verify(hotelService, times(1)).findById(1L);
        verify(roomTypeService, times(1)).findById(1L, 1L);
        verify(bookingService, times(1)).createBooking(any(Booking.class));
    }

    @Test
    @DisplayName("GET /bookings/{id}/cancellation-policy -> should return preview")
    void getCancellationPolicy_ShouldReturnPreview() throws Exception {
        CancellationResponse response = CancellationResponse.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(300))
                .cancellationFee(BigDecimal.valueOf(20))
                .refundPercentage(75)
                .policyMessage("75% refund policy")
                .daysUntilCheckIn(20)
                .build();

        given(bookingService.previewCancellation(1L)).willReturn(response);

        mockMvc.perform(get("/bookings/{id}/cancellation-policy", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.canCancel").value(true));

        verify(bookingService, times(1)).previewCancellation(1L);
    }

    @Test
    @DisplayName("PUT /bookings/{id}/cancel -> should cancel booking")
    void cancelBooking_ShouldReturnCancelledBooking() throws Exception {
        CancellationResponse policy = CancellationResponse.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(240))
                .cancellationFee(BigDecimal.ZERO)
                .refundPercentage(100)
                .policyMessage("Full refund")
                .daysUntilCheckIn(30)
                .build();

        Booking cancelled = buildBooking(1L, 10L, 1L);
        cancelled.setStatus(Booking.BookingStatus.CANCELLED);

        given(bookingService.previewCancellation(1L)).willReturn(policy);
        given(bookingService.cancelBooking(1L, "Travel plans changed")).willReturn(cancelled);

        mockMvc.perform(put("/bookings/{id}/cancel", 1L)
                        .param("reason", "Travel plans changed"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.booking.id").value(1));

        verify(bookingService, times(1)).previewCancellation(1L);
        verify(bookingService, times(1)).cancelBooking(1L, "Travel plans changed");
    }

    @Test
    @DisplayName("PUT /bookings/{id}/cancel -> should return 400 when reason is blank")
    void cancelBooking_WhenReasonBlank_ShouldReturn400() throws Exception {
        mockMvc.perform(put("/bookings/{id}/cancel", 1L)
                        .param("reason", "   "))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).previewCancellation(any());
        verify(bookingService, never()).cancelBooking(any(), any());
    }

    @Test
    @DisplayName("GET /bookings/{id}/rooms -> should return booking rooms")
    void getBookingRooms_ShouldReturnRooms() throws Exception {
        Booking booking = buildBooking(1L, 10L, 1L);

        given(bookingService.getBookingById(1L)).willReturn(booking);

        mockMvc.perform(get("/bookings/{id}/rooms", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roomTypeName").value("Deluxe Room"))
                .andExpect(jsonPath("$[0].numberOfRooms").value(1));

        verify(bookingService, times(1)).getBookingById(1L);
    }

    @Test
    @DisplayName("GET /bookings/reference/{reference} -> should return booking")
    void getBookingByReference_ShouldReturnBooking() throws Exception {
        Booking booking = buildBooking(1L, 10L, 1L);

        given(bookingService.findBookingByReference("WC2026-REF-001")).willReturn(booking);

        mockMvc.perform(get("/bookings/reference/{reference}", "WC2026-REF-001"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookingReference").value("WC2026-REF-001"));

        verify(bookingService, times(1)).findBookingByReference("WC2026-REF-001");
    }

    @Test
    @DisplayName("PUT /bookings/{id} -> should update booking")
    void updateBooking_ShouldReturnUpdatedBooking() throws Exception {
        AppUser user = new AppUser();
        user.setId(10L);

        Hotel hotel = new Hotel();
        hotel.setId(1L);

        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setHotel(hotel);

        Booking updated = buildBooking(1L, 10L, 1L);

        given(appUserService.getUserById(10L)).willReturn(user);
        given(hotelService.findById(1L)).willReturn(hotel);
        given(roomTypeService.findById(1L, 1L)).willReturn(roomType);
        given(bookingService.updateExisting(eq(1L), any(Booking.class))).willReturn(updated);

        String requestBody = """
                {
                  "hotelId": 1,
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

        mockMvc.perform(put("/bookings/{id}", 1L)
                        .principal(jwtAuthenticationToken(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookingReference").value("WC2026-REF-001"));

        verify(appUserService, times(1)).getUserById(10L);
        verify(hotelService, times(1)).findById(1L);
        verify(roomTypeService, times(1)).findById(1L, 1L);
        verify(bookingService, times(1)).updateExisting(eq(1L), any(Booking.class));
    }

    @Test
    @DisplayName("GET /bookings/my/history -> should return paged booking history")
    void getMyHistory_ShouldReturnPagedHistory() throws Exception {
        Booking booking = buildBooking(1L, 10L, 1L);
        Page<Booking> page = new PageImpl<>(List.of(booking), PageRequest.of(0, 10), 1);

        given(bookingService.getGuestHistory(eq(10L), any())).willReturn(page);

        mockMvc.perform(get("/bookings/my/history")
                        .principal(jwtAuthenticationToken(10L))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1));

        verify(bookingService, times(1)).getGuestHistory(eq(10L), any());
    }

    @Test
    @DisplayName("GET /bookings/hotel/{hotelId}/upcoming -> should return upcoming bookings")
    void getUpcoming_ShouldReturnUpcomingBookings() throws Exception {
        Booking booking = buildBooking(1L, 10L, 1L);
        Page<Booking> page = new PageImpl<>(List.of(booking), PageRequest.of(0, 10), 1);

        given(bookingService.getHotelUpcomingBookings(eq(1L), any())).willReturn(page);

        mockMvc.perform(get("/bookings/hotel/{hotelId}/upcoming", 1L)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(bookingService, times(1)).getHotelUpcomingBookings(eq(1L), any());
    }

    @Test
    @DisplayName("GET /bookings -> should return filtered bookings")
    void filterBookings_ShouldReturnPagedResult() throws Exception {
        Booking booking = buildBooking(1L, 10L, 1L);
        Page<Booking> page = new PageImpl<>(List.of(booking), PageRequest.of(0, 10), 1);

        given(bookingService.filterBookings(
                eq(1L),
                isNull(),
                eq(1L),
                isNull(),
                eq(Booking.BookingStatus.CONFIRMED),
                eq(LocalDate.of(2026, 7, 1)),
                eq(LocalDate.of(2026, 7, 10)),
                eq(100.0),
                eq(500.0),
                any()
        )).willReturn(page);

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
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(bookingService, times(1)).filterBookings(
                eq(1L),
                isNull(),
                eq(1L),
                isNull(),
                eq(Booking.BookingStatus.CONFIRMED),
                eq(LocalDate.of(2026, 7, 1)),
                eq(LocalDate.of(2026, 7, 10)),
                eq(100.0),
                eq(500.0),
                any()
        );
    }

    @Test
    @DisplayName("PUT /bookings/{id}/checkin -> should return checked-in booking")
    void checkInBooking_ShouldReturnCheckedInBooking() throws Exception {
        Booking checkedIn = buildBooking(1L, 10L, 1L);
        checkedIn.setStatus(Booking.BookingStatus.CHECKED_IN);

        given(bookingService.checkInBooking(1L)).willReturn(checkedIn);

        mockMvc.perform(put("/bookings/{id}/checkin", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));

        verify(bookingService, times(1)).checkInBooking(1L);
    }

    @Test
    @DisplayName("PUT /bookings/{id}/checkout -> should return checked-out booking")
    void checkOutBooking_ShouldReturnCheckedOutBooking() throws Exception {
        Booking checkedOut = buildBooking(1L, 10L, 1L);
        checkedOut.setStatus(Booking.BookingStatus.CHECKED_OUT);

        given(bookingService.checkOutBooking(1L)).willReturn(checkedOut);

        mockMvc.perform(put("/bookings/{id}/checkout", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"));

        verify(bookingService, times(1)).checkOutBooking(1L);
    }

    @Test
    @DisplayName("PUT /bookings/{id}/manager-cancel/reason/{reason} -> should cancel booking by manager")
    void cancelBookingByManager_ShouldReturnCancelledBooking() throws Exception {
        CancellationResponse policy = CancellationResponse.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(240))
                .cancellationFee(BigDecimal.ZERO)
                .refundPercentage(100)
                .policyMessage("Manager cancellation preview")
                .daysUntilCheckIn(30)
                .build();

        Booking cancelled = buildBooking(1L, 10L, 1L);
        cancelled.setStatus(Booking.BookingStatus.CANCELLED);

        given(bookingService.previewManagerCancellation(1L)).willReturn(policy);
        given(bookingService.cancelBookingByManager(1L, "Hotel renovation", "Hotel manger")).willReturn(cancelled);

        mockMvc.perform(put("/bookings/{id}/manager-cancel/reason/{reason}", 1L, "Hotel renovation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booking.id").value(1));

        verify(bookingService, times(1)).previewManagerCancellation(1L);
        verify(bookingService, times(1)).cancelBookingByManager(1L, "Hotel renovation", "Hotel manger");
    }

    @Test
    @DisplayName("GET /bookings/{id}/manager-cancellation-preview -> should return preview")
    void previewManagerCancellation_ShouldReturnPreview() throws Exception {
        CancellationResponse policy = CancellationResponse.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(240))
                .cancellationFee(BigDecimal.ZERO)
                .refundPercentage(100)
                .policyMessage("Manager cancellation preview")
                .daysUntilCheckIn(30)
                .build();

        given(bookingService.previewManagerCancellation(1L)).willReturn(policy);

        mockMvc.perform(get("/bookings/{id}/manager-cancellation-preview", 1L))
                .andExpect(status().isOk());

        verify(bookingService, times(1)).previewManagerCancellation(1L);
    }

    private JwtAuthenticationToken jwtAuthenticationToken(Long userId) {
        Jwt jwt = new Jwt(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("userId", userId)
        );

        return new JwtAuthenticationToken(jwt);
    }
}