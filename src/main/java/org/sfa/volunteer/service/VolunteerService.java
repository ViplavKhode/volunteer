package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.request.VolunteerUserAvailabilityRequest;
import org.sfa.volunteer.dto.response.*;

//import org.sfa.volunteer.dto.request.UserVolunteerSkillsRequest;
//import org.sfa.volunteer.dto.response.UserVolunteerSkillsResponse;
//import org.sfa.volunteer.model.UserVolunteerSkills;

import java.time.LocalDateTime;
import java.util.List;

public interface VolunteerService {

    PaginationResponse<VolunteerResponse> findAllVolunteersWithPagination(Integer pageNumber, Integer pageSize);

    VolunteerResponse createVolunteer(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteer(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerStep1(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerStep2(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerStep3(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerStep4(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerCompletion(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse getVolunteerByUserId(String userId) throws Exception;

    List<VolunteerUserAvailabilityResponse> updateVolunteerUserAvailability(String userId, List<VolunteerUserAvailabilityRequest> request) throws Exception;

    List<VolunteerUserAvailabilityResponse> getVolunteerUserAvailability(String userId) throws Exception;

    Long getNotificationsCountAfterLastAccessed(String userId);

    NotificationPaginationResponse<NotificationsResponse> getNotificationsList(String userId, int page, int size, LocalDateTime clientRefTime);



//    UserVolunteerSkillsResponse updateSkills(UserVolunteerSkillsRequest request) throws Exception;

//    UserVolunteerSkillsResponse findSkillsList() throws Exception;
}
