package com.worldcup.hotelbooking.availability_pricing.availability;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class AvailabilityController {
    private final AvailabilityServiceImpl availabilityService;

    public AvailabilityController(AvailabilityServiceImpl availabilityService) {
        this.availabilityService = availabilityService;
    }

    // Implement endpoints for checking room availability here
    //@PreAuthorize("hasAnyRole('ADMIN','MANAGER','GUEST')")
    @Operation(summary = "Check availability of a specific room type for given check-in and check-out dates")
    @GetMapping("/availability/room-type/{id}")
    public boolean checkRoomTypeAvailability(@PathVariable long id, @RequestParam LocalDate checkIn, @RequestParam LocalDate checkOut) {

        return availabilityService.checkRoomTypeAvailability(id, checkIn, checkOut);
    }

    //@PreAuthorize("hasAnyRole('ADMIN','MANAGER','GUEST')")
    @Operation(summary = "Get the number of available rooms for a specific room type and given check-in and check-out dates")
    @GetMapping("/availability/room-type/{id}/rooms")
    public int getAvailableRooms(@PathVariable long id, @RequestParam LocalDate checkIn, @RequestParam LocalDate checkOut) {

        return availabilityService.getAvailableRooms(id, checkIn, checkOut);
    }
//        GetMapping("/availability/hotel/{id}/?checkIn=2026-06-10&checkOut=2026-06-12")
//        public boolean checkHotelAvailability(@PathVariable long hotelId, LocalDate checkIn, LocalDate checkOut) {
//            return availabilityService.checkHotelAvailability(hotelId,checkIn, checkOut);
//        }
}
