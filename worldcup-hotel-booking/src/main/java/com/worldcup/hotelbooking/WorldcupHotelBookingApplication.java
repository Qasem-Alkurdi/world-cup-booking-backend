package com.worldcup.hotelbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WorldcupHotelBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorldcupHotelBookingApplication.class, args);
    }

}
