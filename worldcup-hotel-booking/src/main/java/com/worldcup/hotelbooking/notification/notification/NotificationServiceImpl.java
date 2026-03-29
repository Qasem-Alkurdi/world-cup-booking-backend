package com.worldcup.hotelbooking.notification.notification;


import com.worldcup.hotelbooking.user.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;


    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendWelcomeNotification(AppUser user) {
        String subject = "Welcome to WorldCup Hotel Booking!";
        String content = "Dear " + user.getUsername() + ",\n\nThank you for registering. We hope you enjoy your stay!";
        sendEmail(user, subject, content);
    }

    @Async
    @Override
    public void sendBookingConfirmedNotification(AppUser user, String bookingReference) {
        String subject = "Booking Confirmed";
        String content = "Your booking with reference " + bookingReference + " has been confirmed.";
        sendEmail(user, subject, content);
    }

    @Async
    @Override
    public void sendBookingCancelledNotification(AppUser user, String bookingReference, String reason) {
        String subject = "Booking Cancelled";
        String content = "Your booking with reference " + bookingReference + " has been cancelled. Reason: " + reason;
        sendEmail(user, subject, content);
    }

    private void sendEmail(AppUser user, String subject, String content) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setSubject(subject);
        notification.setContent(content);
        notification.setStatus(NotificationStatus.PENDING);
        notification = notificationRepository.save(notification);

        try {
            // Mock email sending – just log
            log.info("MOCK EMAIL to {}: Subject: '{}', Content: '{}'", user.getEmail(), subject, content);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to send mock email to {}", user.getEmail(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
        }
        notificationRepository.save(notification);
    }
}