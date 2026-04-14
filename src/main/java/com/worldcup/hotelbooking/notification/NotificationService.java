package com.worldcup.hotelbooking.notification;

import com.worldcup.hotelbooking.user.AppUser;

public interface NotificationService {
    void sendWelcomeNotification(AppUser user);

    void sendBookingConfirmedNotification(AppUser user, String bookingReference);

    void sendBookingCancelledNotification(AppUser user, String bookingReference, String reason);
}