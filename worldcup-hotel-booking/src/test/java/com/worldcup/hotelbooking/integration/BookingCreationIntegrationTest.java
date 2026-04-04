package com.worldcup.hotelbooking.integration;

import com.worldcup.hotelbooking.BaseIntegrationTest;
import com.worldcup.hotelbooking.auth.LoginRequest;
import com.worldcup.hotelbooking.auth.LoginResponse;
import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.booking.booking.BookingRepository;
import com.worldcup.hotelbooking.booking.booking.BookingRequestDto;
import com.worldcup.hotelbooking.booking.booking.BookingResponseDto;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRequestDto;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.HotelStatus;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.notification.NotificationService;
import com.worldcup.hotelbooking.tournament.match.Match;
import com.worldcup.hotelbooking.tournament.match.Match.MatchStage;
import com.worldcup.hotelbooking.tournament.match.MatchRepository;
import com.worldcup.hotelbooking.tournament.stadium.Stadium;
import com.worldcup.hotelbooking.tournament.stadium.StadiumRepository;
import com.worldcup.hotelbooking.user.AppUser;
import com.worldcup.hotelbooking.user.AppUserRepository;
import com.worldcup.hotelbooking.user.AppUserRequestDto;
import com.worldcup.hotelbooking.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test-local")
@Transactional
public class BookingCreationIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private StadiumRepository stadiumRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoBean
    private NotificationService notificationService;

    private Long hotelId;
    private Long roomTypeId;
    private Long userId;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        Stadium stadium = new Stadium();
        stadium.setName("Test Stadium");
        stadium.setCity("Test City");
        stadium.setLatitude(40.7128);
        stadium.setLongitude(-74.0060);
        stadium = stadiumRepository.save(stadium);

        Match match = new Match();
        match.setHomeTeam("Brazil");
        match.setAwayTeam("Germany");
        match.setMatchDateTime(LocalDateTime.of(2026, 6, 15, 20, 0));
        match.setStage(MatchStage.GROUP_STAGE_1);
        match.setStadium(stadium);
        match.setOpeningMatch(false);
        match = matchRepository.save(match);

        AppUser owner = new AppUser();
        owner.setUsername("hotelowner");
        owner.setEmail("owner@test.com");
        owner.setPassword(passwordEncoder.encode("Owner@123"));
        owner.setRoles(Set.of(Role.MANAGER));
        owner.setEnabled(true);
        owner = userRepository.save(owner);

        GeometryFactory gf = new GeometryFactory();
        Point location = gf.createPoint(new Coordinate(-74.0060, 40.7128));
        location.setSRID(4326);

        Hotel hotel = new Hotel();
        hotel.setName("World Cup Stadium Hotel");
        hotel.setDescription("Near the stadium");
        hotel.setOwner(owner);
        hotel.setCountry("USA");
        hotel.setCity("Test City");
        hotel.setAddressLine("123 Main St");
        hotel.setLocation(location);
        hotel.setStatus(HotelStatus.APPROVED);
        hotel.setDeleted(false);
        hotel = hotelRepository.save(hotel);
        hotelId = hotel.getId();

        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        roomType.setName("Standard Double");
        roomType.setDescription("Comfortable double room");
        roomType.setMaxAdults(2);
        roomType.setMaxChildren(1);
        roomType.setBasePrice(new BigDecimal("150.00"));
        roomType.setTotalRooms(10);
        roomType.setCurrency("USD");
        roomType = roomTypeRepository.save(roomType);
        roomTypeId = roomType.getId();

        AppUserRequestDto registerDto =
                new AppUserRequestDto("testguest", "guest@test.com", "Xy7!kLm#92Qa");

        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andReturn();

        userId = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .get("ID")
                .asLong();

        LoginRequest loginRequest = new LoginRequest("testguest", "Xy7!kLm#92Qa");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponse.class
        );

        jwtToken = loginResponse.accessToken();
    }

    @Test
    void createBooking_shouldSucceedWithCorrectPricing() throws Exception {
        BookingRoomRequestDto roomRequest = new BookingRoomRequestDto();
        roomRequest.setRoomTypeId(roomTypeId);
        roomRequest.setNumberOfRooms(1);

        BookingRequestDto bookingRequest = new BookingRequestDto();
        bookingRequest.setHotelId(hotelId);
        bookingRequest.setCheckInDate(LocalDate.of(2026, 6, 14));
        bookingRequest.setCheckOutDate(LocalDate.of(2026, 6, 17));
        bookingRequest.setNumberOfGuests(2);
        bookingRequest.setNumberOfAdults(2);
        bookingRequest.setNumberOfChildren(0);
        bookingRequest.setRooms(List.of(roomRequest));

        MvcResult result = mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        BookingResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BookingResponseDto.class
        );

        assertThat(response.getId()).isNotNull();
        assertThat(response.getBookingReference()).startsWith("WC2026-");
        assertThat(response.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
        assertThat(response.getCheckInDate()).isEqualTo(LocalDate.of(2026, 6, 14));
        assertThat(response.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 6, 17));
        assertThat(response.getTotalPrice()).isNotNull();
        assertThat(response.getTotalPrice().compareTo(BigDecimal.ZERO)).isGreaterThanOrEqualTo(0);

        assertThat(response.getRooms()).hasSize(1);
        assertThat(response.getRooms().get(0).getRoomTypeName()).isEqualTo("Standard Double");
        assertThat(response.getRooms().get(0).getNumberOfRooms()).isEqualTo(1);

        Booking savedBooking = bookingRepository.findByIdWithRooms(response.getId()).orElseThrow();
        assertThat(savedBooking.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
        assertThat(savedBooking.getTotalPrice()).isEqualByComparingTo(response.getTotalPrice());
        assertThat(savedBooking.getBookingRooms()).hasSize(1);
        assertThat(savedBooking.getBookingRooms().get(0).getRoomType().getId()).isEqualTo(roomTypeId);
    }

    @Test
    void createBooking_shouldFailWhenNoAvailability() throws Exception {
        BookingRoomRequestDto roomRequest = new BookingRoomRequestDto();
        roomRequest.setRoomTypeId(roomTypeId);
        roomRequest.setNumberOfRooms(10);

        BookingRequestDto firstBooking = new BookingRequestDto();
        firstBooking.setHotelId(hotelId);
        firstBooking.setCheckInDate(LocalDate.of(2026, 6, 14));
        firstBooking.setCheckOutDate(LocalDate.of(2026, 6, 17));
        firstBooking.setNumberOfGuests(2);
        firstBooking.setNumberOfAdults(2);
        firstBooking.setNumberOfChildren(0);
        firstBooking.setRooms(List.of(roomRequest));

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstBooking)))
                .andExpect(status().isCreated());

        BookingRoomRequestDto roomRequest2 = new BookingRoomRequestDto();
        roomRequest2.setRoomTypeId(roomTypeId);
        roomRequest2.setNumberOfRooms(1);

        BookingRequestDto secondBooking = new BookingRequestDto();
        secondBooking.setHotelId(hotelId);
        secondBooking.setCheckInDate(LocalDate.of(2026, 6, 14));
        secondBooking.setCheckOutDate(LocalDate.of(2026, 6, 17));
        secondBooking.setNumberOfGuests(2);
        secondBooking.setNumberOfAdults(2);
        secondBooking.setNumberOfChildren(0);
        secondBooking.setRooms(List.of(roomRequest2));

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondBooking)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.CoreMatchers.containsString("Not enough rooms available")));
    }
}