package com.worldcup.hotelbooking.availability_pricing.availability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class AvailabilityController {
    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    // Implement endpoints for checking room availability here
    @GetMapping("/availability/room-type/{id}")
    public boolean checkRoomTypeAvailability(@PathVariable long id, @RequestParam LocalDate checkIn, @RequestParam LocalDate checkOut) {
        return availabilityService.checkRoomTypeAvailability(id, checkIn, checkOut);
    }

    @GetMapping("/availability/room-type/{id}/room")
    public int getAvailableRooms(@PathVariable long id, @RequestParam LocalDate checkIn, @RequestParam LocalDate checkOut) {
        return availabilityService.getAvailableRooms(id, checkIn, checkOut);
    }
//        GetMapping("/availability/hotel/{id}/?checkIn=2026-06-10&checkOut=2026-06-12")
//        public boolean checkHotelAvailability(@PathVariable long hotelId, LocalDate checkIn, LocalDate checkOut) {
//            return availabilityService.checkHotelAvailability(hotelId,checkIn, checkOut);
//        }
}
