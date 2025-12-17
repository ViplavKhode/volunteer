package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.response.NotificationsResponse;
import org.sfa.volunteer.dto.response.UserProfileResponse;

import java.util.Date;
import java.util.List;

public interface RequestService {

//Actual methods to be used ----
    Long getNotificationsCountAfterLastAccessed(String userId);

    List<NotificationsResponse> getNotificationsList(String userId);


}
