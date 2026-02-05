package org.sfa.volunteer.service.impl;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.ArgumentCaptor;
import org.sfa.volunteer.dto.response.NotificationPaginationResponse;
import org.sfa.volunteer.dto.response.NotificationsResponse;
import org.sfa.volunteer.model.NotificationTypes;
import org.sfa.volunteer.model.Notifications;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.UserNotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.repository.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class VolunteerServiceImplTest {

    @Mock private VolunteerRepository volunteerRepository;
    @Mock private UserRepository userRepository;
    @Mock private VolunteerUserAvailabilityRepository userAvailabilityRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private UserNotificationStatusRepository userNotificationStatusRepository;

    @Spy @InjectMocks private VolunteerServiceImpl volunteerService;


    @Test
    void getNotificationsCountAfterLastAccessed_whenLastAccessedExists_usesThatTime() {
        // arrange
        String userId = "u1";
        LocalDateTime lastAccessed = LocalDateTime.of(2025, 1, 10, 12, 0);

        when(userNotificationStatusRepository.findLastAccessedTimeByUser(userId))
                .thenReturn(Optional.of(lastAccessed));
        when(notificationRepository.countByUser_IdAndCreatedAtAfter(userId, lastAccessed))
                .thenReturn(5L);

        // act
        Long result = volunteerService.getNotificationsCountAfterLastAccessed(userId);

        // assert
        assertEquals(5L, result);

        verify(userNotificationStatusRepository).findLastAccessedTimeByUser(userId);
        verify(notificationRepository).countByUser_IdAndCreatedAtAfter(userId, lastAccessed);
        verifyNoMoreInteractions(userNotificationStatusRepository, notificationRepository);
    }

    @Test
    void getNotificationsCountAfterLastAccessed_whenLastAccessedMissing_usesEpochDefault() {
        // arrange
        String userId = "u1";
        LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0);

        when(userNotificationStatusRepository.findLastAccessedTimeByUser(userId))
                .thenReturn(Optional.empty());
        when(notificationRepository.countByUser_IdAndCreatedAtAfter(userId, epoch))
                .thenReturn(12L);

        // act
        Long result = volunteerService.getNotificationsCountAfterLastAccessed(userId);

        // assert
        assertEquals(12L, result);

        verify(userNotificationStatusRepository).findLastAccessedTimeByUser(userId);
        verify(notificationRepository).countByUser_IdAndCreatedAtAfter(userId, epoch);
        verifyNoMoreInteractions(userNotificationStatusRepository, notificationRepository);
    }

    // // Unit test: verifies first-page notifications fetch (clientRefTime=null) uses DB lastAccessedTime, avoids real DB update via spy,
    // maps Notifications -> NotificationsResponse correctly, and sets status=true when createdAt is after lastAccessed.
    @Test
    void getNotificationsList_firstPage_noClientRefTime_basicFlow() {

        // -------- Arrange (setup mocks and input) --------

        String userId = "u1";
        int page = 0;
        int size = 10;

        // This represents the user's last time opening notifications (from DB)
        LocalDateTime dbLastAccessed = LocalDateTime.of(2025, 1, 1, 10, 0);

        // Build NotificationTypes entity (used inside Notifications)
        NotificationTypes type = NotificationTypes.builder()
                .type_name("INFO")
                .description("Test title")
                .build();

        // Build a single notification created AFTER lastAccessed
        Notifications notification = Notifications.builder()
                .type(type)
                .message("Test message")
                .createdAt(LocalDateTime.of(2025, 1, 1, 11, 0))
                .build();

        // Repository returns a page containing this notification
        Page<Notifications> mockPage = new PageImpl<>(List.of(notification));

        // DB returns lastAccessed timestamp
        when(userNotificationStatusRepository.findLastAccessedTimeByUser(userId))
                .thenReturn(Optional.of(dbLastAccessed));

        // We DO NOT want to actually update DB in unit test
        doNothing().when(volunteerService)
                .updateUserNewLastAccessedTime(eq(userId), any(LocalDateTime.class));

        // Repository returns notifications page
        when(notificationRepository.findByUserIdAndCreatedAtLessThanEqual(
                eq(userId), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        // -------- Act (call the method under test) --------

        NotificationPaginationResponse<NotificationsResponse> response =
                volunteerService.getNotificationsList(userId, page, size, null);

        // -------- Assert (verify output mapping + logic) --------

        // One notification returned
        assertEquals(1, response.getItems().size());

        // Fields correctly mapped from entity to response DTO
        assertEquals("INFO", response.getItems().get(0).getType());
        assertEquals("Test title", response.getItems().get(0).getTitle());
        assertEquals("Test message", response.getItems().get(0).getMessage());

        // createdAt (11:00) is AFTER lastAccessed (10:00) → status = true
        assertEquals(true, response.getItems().get(0).getStatus());

        // referenceTimestamp must be DB lastAccessed
        assertEquals(dbLastAccessed, response.getReferenceTimestamp());
    }

    // Unit test: verifies paginated notification fetch with clientRefTime uses clientRefTime for status/referenceTimestamp,
    // does NOT update lastAccessed in DB, and maps notifications correctly.
    @Test
    void getNotificationsList_whenClientRefTimeProvided_doesNotUpdate_andUsesClientRefForStatus() {

        // Arrange
        String userId = "u1";
        int page = 1;
        int size = 10;

        // clientRefTime is what UI passes back after first fetch
        LocalDateTime clientRefTime = LocalDateTime.of(2025, 1, 1, 10, 0);

        // DB last accessed (used ONLY for filtering repo query in your else-branch)
        LocalDateTime dbLastAccessed = LocalDateTime.of(2025, 1, 1, 9, 0);

        NotificationTypes type = NotificationTypes.builder()
                .type_name("INFO")
                .description("Test title")
                .build();

        // createdAt is AFTER clientRefTime => status should be true
        Notifications notification = Notifications.builder()
                .type(type)
                .message("Test message")
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 5))
                .build();

        Page<Notifications> mockPage = new PageImpl<>(List.of(notification));

        when(userNotificationStatusRepository.findLastAccessedTimeByUser(userId))
                .thenReturn(Optional.of(dbLastAccessed));

        when(notificationRepository.findByUserIdAndCreatedAtLessThanEqual(
                eq(userId),
                eq(Optional.of(dbLastAccessed)),
                any(Pageable.class)))
                .thenReturn(mockPage);

        // Act
        NotificationPaginationResponse<NotificationsResponse> response =
                volunteerService.getNotificationsList(userId, page, size, clientRefTime);

        // Assert
        assertEquals(1, response.getItems().size());
        assertEquals(true, response.getItems().get(0).getStatus());

        // IMPORTANT: referenceTimestamp should be clientRefTime in this branch
        assertEquals(clientRefTime, response.getReferenceTimestamp());

        // IMPORTANT: should NOT update last accessed time in this branch
        verify(volunteerService, never()).updateUserNewLastAccessedTime(anyString(), any(LocalDateTime.class));

        // Repo interactions
        verify(userNotificationStatusRepository).findLastAccessedTimeByUser(userId);
        verify(notificationRepository).findByUserIdAndCreatedAtLessThanEqual(
                eq(userId),
                eq(Optional.of(dbLastAccessed)),
                any(Pageable.class)
        );
    }

    // Unit test: verifies first-page notification fetch when DB has no lastAccessedTime falls back to epoch,
    // updates lastAccessed, and marks all notifications as new.
    @Test
    void getNotificationsList_firstPage_noClientRefTime_whenDbMissing_usesEpochReferenceAndStatusLogic() {

        // Arrange
        String userId = "u1";
        int page = 0;
        int size = 10;

        // DB returns empty => service should use epoch as lastAccessedTime
        LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0);

        NotificationTypes type = NotificationTypes.builder()
                .type_name("INFO")
                .description("Test title")
                .build();

        // Any realistic createdAt will be after epoch => status should be true
        Notifications notification = Notifications.builder()
                .type(type)
                .message("Test message")
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 5))
                .build();

        Page<Notifications> mockPage = new PageImpl<>(List.of(notification));

        when(userNotificationStatusRepository.findLastAccessedTimeByUser(userId))
                .thenReturn(Optional.empty());

        // Prevent real DB update call
        doNothing().when(volunteerService)
                .updateUserNewLastAccessedTime(eq(userId), any(LocalDateTime.class));

        when(notificationRepository.findByUserIdAndCreatedAtLessThanEqual(
                eq(userId), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        // Act
        NotificationPaginationResponse<NotificationsResponse> response =
                volunteerService.getNotificationsList(userId, page, size, null);

        // Assert
        assertEquals(epoch, response.getReferenceTimestamp());
        assertEquals(1, response.getItems().size());
        assertEquals(true, response.getItems().get(0).getStatus());

        verify(volunteerService).updateUserNewLastAccessedTime(eq(userId), any(LocalDateTime.class));
        verify(userNotificationStatusRepository).findLastAccessedTimeByUser(userId);
    }

    // Unit test: verifies existing UserNotificationStatus updates lastAccessedAt and is saved.
    @Test
    void updateUserNewLastAccessedTime_whenStatusExists_updatesLastAccessedAt() {

        String userId = "u1";
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);

        UserNotificationStatus existingStatus = UserNotificationStatus.builder()
                .id(userId)
                .lastAccessedAt(now.minusDays(1))
                .build();

        when(userNotificationStatusRepository.findByUserId(userId))
                .thenReturn(existingStatus);

        volunteerService.updateUserNewLastAccessedTime(userId, now);

        assertEquals(now, existingStatus.getLastAccessedAt());
        verify(userNotificationStatusRepository).save(existingStatus);
    }


    // Unit test: verifies missing status + existing user creates a new UserNotificationStatus with user + lastAccessedAt and saves it.
    @Test
    void updateUserNewLastAccessedTime_whenStatusMissing_andUserExists_createsNewStatusAndSaves() {

        // Arrange
        String userId = "u1";
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);

        when(userNotificationStatusRepository.findByUserId(userId))
                .thenReturn(null);

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        ArgumentCaptor<UserNotificationStatus> captor =
                ArgumentCaptor.forClass(UserNotificationStatus.class);

        // Act
        volunteerService.updateUserNewLastAccessedTime(userId, now);

        // Assert
        verify(userNotificationStatusRepository).save(captor.capture());

        UserNotificationStatus saved = captor.getValue();
        assertEquals(now, saved.getLastAccessedAt());
        assertEquals(userId, saved.getUser().getId());
    }

    // Unit test: verifies missing status + missing user returns early and does not save anything.
    @Test
    void updateUserNewLastAccessedTime_whenStatusMissing_andUserMissing_doesNotSave() {

        String userId = "u1";
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);

        when(userNotificationStatusRepository.findByUserId(userId))
                .thenReturn(null);

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        volunteerService.updateUserNewLastAccessedTime(userId, now);

        verify(userNotificationStatusRepository, never()).save(any());
    }


}
