package com.worldcup.hotelbooking.notification;

import com.worldcup.hotelbooking.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // NotificationRepository.java
    void deleteByUser(AppUser user);
}
