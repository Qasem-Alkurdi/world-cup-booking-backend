package com.worldcup.hotelbooking.notification.notification;

import com.worldcup.hotelbooking.user.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
    }

    @Test
    void sendWelcomeNotification_success() {
        // Simulate first save returns a PENDING notification
        Notification firstSaved = new Notification();
        firstSaved.setId(1L);
        firstSaved.setStatus(NotificationStatus.PENDING);

        // Simulate second save returns a SENT notification
        Notification secondSaved = new Notification();
        secondSaved.setId(1L);
        secondSaved.setStatus(NotificationStatus.SENT);
        secondSaved.setSentAt(LocalDateTime.now());

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(firstSaved)   // first call
                .thenReturn(secondSaved); // second call

        notificationService.sendWelcomeNotification(user);

        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        Notification first = notificationCaptor.getAllValues().get(0);
        Notification second = notificationCaptor.getAllValues().get(1);

        assertThat(first.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(second.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(second.getSentAt()).isNotNull();
    }


}