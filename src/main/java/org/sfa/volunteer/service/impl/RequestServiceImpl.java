package org.sfa.volunteer.service.impl;

import org.sfa.volunteer.dto.response.NotificationsResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.Notifications;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.UserNotificationStatus;
import org.sfa.volunteer.repository.NotificationRepository;
import org.sfa.volunteer.repository.UserNotificationStatusRepository;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RequestServiceImpl implements RequestService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserNotificationStatusRepository userNotificationStatusRepository;
    public RequestServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository, UserNotificationStatusRepository userNotificationStatusRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.userNotificationStatusRepository = userNotificationStatusRepository;
    }


    public List<NotificationsResponse> getNotificationsList(String userId){
        LocalDateTime newLastAccessedTime = LocalDateTime.now();
        updateUserNewLastAccessedTime(userId, newLastAccessedTime);
        //       step1 get the lastacessed time for the userId
        Optional<LocalDateTime> lastAccessedTimeOptional = userNotificationStatusRepository.findLastAccessedTimeByUser(userId);
//        step2 get the count of notifications after the last accessed time
        LocalDateTime lastAccessedTime = lastAccessedTimeOptional
                .orElse(LocalDateTime.now().minusDays(30));

        List<Notifications> notificationsList = notificationRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, lastAccessedTime);
        List<NotificationsResponse> notificationsResponseList = notificationsList.stream().map(notifications -> NotificationsResponse.builder().type(notifications.getType().getType_name())
                .title(notifications.getType().getDescription())
                .message(notifications.getMessage())
                .date(notifications.getCreatedAt())
                .build()
        ).collect(Collectors.toList());

        return notificationsResponseList;
    }
// function to use
    public void updateUserNewLastAccessedTime(String userId, LocalDateTime newTime){
        UserNotificationStatus userNotificationStatus = userNotificationStatusRepository.findByUserId(userId);
        if (userNotificationStatus == null) {
            // Create new record if it doesn't exist
            User user = userRepository.findById(userId).orElseThrow();
            userNotificationStatus = UserNotificationStatus.builder()
                    .user(user)
                    .lastAccessedAt(newTime)
                    .build();
        } else {
            // Update existing record
            userNotificationStatus.setLastAccessedAt(newTime);
        }
        userNotificationStatusRepository.save(userNotificationStatus);

    }

//    Final method for getting the notifications count
    public Long getNotificationsCountAfterLastAccessed(String userId){
//       step1 get the lastacessed time for the userId
        Optional<LocalDateTime> lastAccessedTimeOptional = userNotificationStatusRepository.findLastAccessedTimeByUser(userId);
//        step2 get the count of notifications after the last accessed time
        LocalDateTime lastAccessedTime = lastAccessedTimeOptional
                .orElse(LocalDateTime.now().minusDays(30));
        Long notificationsCount = notificationRepository.countByUserIdAndCreatedAtAfter(userId, lastAccessedTime);
        return notificationsCount;
    }

}
