package com.worldcup.hotelbooking.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheConfig.CacheTtlProperties.class)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(CacheTtlProperties ttl) {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Each cache gets its own TTL and a size cap (Caffeine evicts LRU when full)
        manager.registerCustomCache("hotelList",
                buildCaffeine(ttl.getHotelList(), 1).build());

        manager.registerCustomCache("hotelById",
                buildCaffeine(ttl.getHotelById(), 500).build());

        manager.registerCustomCache("myHotels",
                buildCaffeine(ttl.getMyHotels(), 200).build());

        manager.registerCustomCache("hotelPhotos",
                buildCaffeine(ttl.getHotelPhotos(), 500).build());

        manager.registerCustomCache("roomTypesByHotel",
                buildCaffeine(ttl.getRoomTypesByHotel(), 500).build());

        manager.registerCustomCache("roomTypeById",
                buildCaffeine(ttl.getRoomTypeById(), 1000).build());

        manager.registerCustomCache("roomTypePhotos",
                buildCaffeine(ttl.getRoomTypePhotos(), 1000).build());

        manager.registerCustomCache("bookingById",
                buildCaffeine(ttl.getBookingById(), 2000).build());

        manager.registerCustomCache("bookingByReference",
                buildCaffeine(ttl.getBookingByReference(), 2000).build());

        manager.registerCustomCache("guestHistory",
                buildCaffeine(ttl.getGuestHistory(), 500).build());

        manager.registerCustomCache("hotelUpcoming",
                buildCaffeine(ttl.getHotelUpcoming(), 500).build());

        manager.registerCustomCache("hibpSuffixes",
                buildCaffeine(ttl.getHibpSuffixes(), 1000).build());

        return manager;
    }

    private Caffeine<Object, Object> buildCaffeine(int ttlMinutes, long maximumSize) {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .maximumSize(maximumSize);
    }

    // Reads all app.cache.ttl.* values from application.yml
    @ConfigurationProperties(prefix = "app.cache.ttl")
    public static class CacheTtlProperties {
        private int hotelList = 10;
        private int hotelById = 10;
        private int myHotels = 10;
        private int hotelPhotos = 10;
        private int roomTypesByHotel = 10;
        private int roomTypeById = 10;
        private int roomTypePhotos = 10;
        private int bookingById = 5;
        private int bookingByReference = 5;
        private int guestHistory = 5;
        private int hotelUpcoming = 5;
        private int hibpSuffixes = 60;

        public int getHotelList() {
            return hotelList;
        }

        public void setHotelList(int v) {
            hotelList = v;
        }

        public int getHotelById() {
            return hotelById;
        }

        public void setHotelById(int v) {
            hotelById = v;
        }

        public int getMyHotels() {
            return myHotels;
        }

        public void setMyHotels(int v) {
            myHotels = v;
        }

        public int getHotelPhotos() {
            return hotelPhotos;
        }

        public void setHotelPhotos(int v) {
            hotelPhotos = v;
        }

        public int getRoomTypesByHotel() {
            return roomTypesByHotel;
        }

        public void setRoomTypesByHotel(int v) {
            roomTypesByHotel = v;
        }

        public int getRoomTypeById() {
            return roomTypeById;
        }

        public void setRoomTypeById(int v) {
            roomTypeById = v;
        }

        public int getRoomTypePhotos() {
            return roomTypePhotos;
        }

        public void setRoomTypePhotos(int v) {
            roomTypePhotos = v;
        }

        public int getBookingById() {
            return bookingById;
        }

        public void setBookingById(int v) {
            bookingById = v;
        }

        public int getBookingByReference() {
            return bookingByReference;
        }

        public void setBookingByReference(int v) {
            bookingByReference = v;
        }

        public int getGuestHistory() {
            return guestHistory;
        }

        public void setGuestHistory(int v) {
            guestHistory = v;
        }

        public int getHotelUpcoming() {
            return hotelUpcoming;
        }

        public void setHotelUpcoming(int v) {
            hotelUpcoming = v;
        }

        public int getHibpSuffixes() {
            return hibpSuffixes;
        }

        public void setHibpSuffixes(int v) {
            hibpSuffixes = v;
        }
    }
}