package org.sfa.volunteer.service.impl;
import jakarta.transaction.Transactional;
//import org.sfa.volunteer.dto.request.UserVolunteerSkillsRequest;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.request.VolunteerUserAvailabilityRequest;
//import org.sfa.volunteer.dto.response.UserVolunteerSkillsResponse;
//import org.sfa.volunteer.model.UserVolunteerSkills;
//import org.sfa.volunteer.repository.UserVolunteerSkillsRepository;
import org.sfa.volunteer.dto.response.*;
import org.sfa.volunteer.model.*;
import org.sfa.volunteer.repository.*;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.exception.VolunteerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.sfa.volunteer.service.VolunteerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class VolunteerServiceImpl implements VolunteerService {
    private final VolunteerRepository volunteerRepository;
    private final UserRepository userRepository;
    private final VolunteerUserAvailabilityRepository userAvailabilityRepository;
    private final NotificationRepository notificationRepository;
    private final UserNotificationStatusRepository userNotificationStatusRepository;

//    private final UserVolunteerSkillsRepository userVolunteerSkillsRepository;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    @Autowired
    public VolunteerServiceImpl(VolunteerRepository volunteerRepository, UserRepository userRepository,
            VolunteerUserAvailabilityRepository volunteerUserAvailabilityRepository,
            VolunteerUserAvailabilityRepository userAvailabilityRepository, NotificationRepository notificationRepository, UserNotificationStatusRepository userNotificationStatusRepository) {
        this.userRepository = userRepository;
        this.volunteerRepository = volunteerRepository;
        this.userAvailabilityRepository = userAvailabilityRepository;
        this.notificationRepository = notificationRepository;
        this.userNotificationStatusRepository = userNotificationStatusRepository;
//        this.userVolunteerSkillsRepository = userVolunteerSkillsRepository;
    }

    private void updateUser(User user, Integer step) {
        user.setVolunteerStage(step);
        user.setVolunteerUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));
        user = userRepository.save(user);
    }

    @Override
    public VolunteerResponse createVolunteer(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findVolunteerByUserId(request.userId());
        if (Objects.nonNull(volunteer)) {
            throw VolunteerException.volunteerExists(request.userId());
        }
        if (request.step() != 1)
            throw VolunteerException.volunteerInvalidStep(request.userId());
        else {
            volunteer = Volunteer.builder()
                    .user(user)
                    .termsAndConditions(request.termsAndConditions())
                    .tcUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")))
                    .build();
            volunteer = volunteerRepository.save(volunteer);
            updateUser(user, request.step());
            return mapToVolunteerResponse(volunteer);
        }
    }

    @Override
    public VolunteerResponse updateVolunteer(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        VolunteerResponse response = null;
        if (request.step() == 1)
            response = updateVolunteerStep1(request);
        else if (request.step() == 2)
            response = updateVolunteerStep2(request);
        else if (request.step() == 3)
            response = updateVolunteerStep3(request);
        else if (request.step() == 4)
            response = updateVolunteerStep4(request);
        return response;
    }

    @Override
    public VolunteerResponse updateVolunteerStep1(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findVolunteerByUserId(request.userId());
        if (Objects.isNull(volunteer)) {
            throw VolunteerException.volunteerNotFound(request.userId());
        }
        volunteer.setUser(user);
        if (request.step() != 1)
            throw VolunteerException.volunteerInvalidStep(request.userId());

        volunteer.setTermsAndConditions(request.termsAndConditions());
        volunteer.setTcUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));
        volunteer = volunteerRepository.save(volunteer);

        updateUser(user, request.step());

        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public VolunteerResponse updateVolunteerStep2(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findVolunteerByUserId(request.userId());
        if (Objects.isNull(volunteer)) {
            throw VolunteerException.volunteerNotFound(request.userId());
        }
        volunteer.setUser(user);
        if (request.step() != 2)
            throw VolunteerException.volunteerInvalidStep(request.userId());

        volunteer.setGovtIdFilename(request.govtIdFilename());
        volunteer.setGovtUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));
        volunteer = volunteerRepository.save(volunteer);

        updateUser(user, request.step());

        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public VolunteerResponse updateVolunteerStep3(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findVolunteerByUserId(request.userId());
        if (Objects.isNull(volunteer)) {
            throw VolunteerException.volunteerNotFound(request.userId());
        }
        volunteer.setUser(user);
        if (request.step() != 3)
            throw VolunteerException.volunteerInvalidStep(request.userId());

        volunteer.setSkills(request.skills());
        volunteer = volunteerRepository.save(volunteer);

        updateUser(user, request.step());

        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public VolunteerResponse updateVolunteerStep4(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findVolunteerByUserId(request.userId());
        if (Objects.isNull(volunteer)) {
            throw VolunteerException.volunteerNotFound(request.userId());
        }
        volunteer.setUser(user);
        if (request.step() != 4)
        throw VolunteerException.volunteerInvalidStep(request.userId());

        volunteer.setNotification(request.notification());
        volunteer.setIsCompleted(request.isCompleted());
        volunteer.setCompletedDate(ZonedDateTime.now(ZoneId.of("UTC")));
        volunteer = volunteerRepository.save(volunteer);

        List<VolunteerUserAvailability> userAvailabilityList = request.availability().stream()
                .map(req -> mapToVolunteerUserAvailability(req, user)).collect(Collectors.toList());

        List<VolunteerUserAvailability> savedAvailabilityList = userAvailabilityRepository
                .saveAll(userAvailabilityList);

        updateUser(user, request.step());

        return mapToVolunteerResponse(volunteer, savedAvailabilityList);
    }

    @Override
    public List<VolunteerUserAvailabilityResponse> updateVolunteerUserAvailability(String userId,
            List<VolunteerUserAvailabilityRequest> request) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Volunteer volunteer = volunteerRepository.findVolunteerByUserId(userId);
        if (Objects.isNull(volunteer)) {
            throw VolunteerException.volunteerNotFound(userId);
        }

        List<VolunteerUserAvailability> userAvailabilityList = request.stream()
                .map(req -> mapToVolunteerUserAvailability(req, user)).collect(Collectors.toList());

        List<VolunteerUserAvailability> savedAvailabilityList = userAvailabilityRepository
                .saveAll(userAvailabilityList);
        return savedAvailabilityList.stream().map(this::mapToVolunteerUserAvailabilityResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<VolunteerUserAvailabilityResponse> getVolunteerUserAvailability(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<VolunteerUserAvailability> availabilityList = userAvailabilityRepository.findUserAvailability(userId);
        List<VolunteerUserAvailabilityResponse> availability = availabilityList.stream()
                .map(this::mapToVolunteerUserAvailabilityResponse)
                .collect(Collectors.toList());

        return availability;
    }

//    @Override
//    public UserVolunteerSkillsResponse findSkillsList() throws Exception {
//        return null;
//    }

    @Override
    public VolunteerResponse updateVolunteerCompletion(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findVolunteerByUserId(request.userId());
        if (Objects.isNull(volunteer)) {
            throw VolunteerException.volunteerNotFound(request.userId());
        }

        volunteer.setIsCompleted(request.isCompleted());
        volunteer.setCompletedDate(ZonedDateTime.now(ZoneId.of("UTC")));
        volunteer = volunteerRepository.save(volunteer);

        Integer step = 4;
        if (Objects.nonNull(request.step()))
            step = request.step();

        updateUser(user, step);

        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public VolunteerResponse getVolunteerByUserId(String userId) throws Exception {
        Volunteer volunteer = volunteerRepository.findVolunteerByUserId(userId);
        if (volunteer == null || volunteer.getId() == 0) {
            throw VolunteerException.volunteerNotFound(userId);
        }
        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public PaginationResponse<VolunteerResponse> findAllVolunteersWithPagination(Integer pageNumber, Integer pageSize) {
        int pageNum = (pageNumber == null) ? DEFAULT_PAGE : pageNumber;
        int pageSizeNum = (pageSize == null) ? DEFAULT_SIZE : pageSize;
        Pageable pageable = PageRequest.of(pageNum, pageSizeNum);
        Page<Volunteer> volunteerPage = volunteerRepository.findAll(pageable);

        List<VolunteerResponse> volunteers = volunteerPage.stream()
                .map(this::mapToVolunteerResponse)
                .collect(Collectors.toList());

        return PaginationResponse.<VolunteerResponse>builder()
                .currentPage(volunteerPage.getNumber())
                .pageSize(volunteerPage.getSize())
                .totalPages(volunteerPage.getTotalPages())
                .totalItems(volunteerPage.getTotalElements())
                .items(volunteers)
                .hasNextPage(volunteerPage.hasNext())
                .hasPreviousPage(volunteerPage.hasPrevious())
                .build();
    }

    private VolunteerResponse mapToVolunteerResponse(Volunteer volunteer) {
        return VolunteerResponse.builder()
                .id(volunteer.getId())
                .userId(volunteer.getUser().getId())
                .termsAndConditions(volunteer.getTermsAndConditions())
                .tcUpdateDate(volunteer.getTcUpdateDate())
                .govtIdFilename(volunteer.getGovtIdFilename())
                .govtUpdateDate(volunteer.getGovtUpdateDate())
                .skills(volunteer.getSkills())
                .notification(volunteer.getNotification())
                .isCompleted(volunteer.getIsCompleted())
                .completedDate(volunteer.getCompletedDate())
                .build();
    }

    private VolunteerResponse mapToVolunteerResponse(Volunteer volunteer,
            List<VolunteerUserAvailability> availabilityList) {
        return VolunteerResponse.builder()
                .id(volunteer.getId())
                .userId(volunteer.getUser().getId())
                .termsAndConditions(volunteer.getTermsAndConditions())
                .tcUpdateDate(volunteer.getTcUpdateDate())
                .govtIdFilename(volunteer.getGovtIdFilename())
                .govtUpdateDate(volunteer.getGovtUpdateDate())
                .skills(volunteer.getSkills())
                .notification(volunteer.getNotification())
                .isCompleted(volunteer.getIsCompleted())
                .completedDate(volunteer.getCompletedDate())
                .availability(availabilityList.stream().map(this::mapToVolunteerUserAvailabilityResponse).toList())
                .build();
    }

    private VolunteerUserAvailabilityResponse mapToVolunteerUserAvailabilityResponse(
            VolunteerUserAvailability availability) {
        return VolunteerUserAvailabilityResponse.builder()
                .id(availability.getId())
                .userId(availability.getUser().getId())
                .dayOfWeek(availability.getDayOfWeek())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .lastUpdateDate(availability.getLastUpdateDate())
                .build();
    }

    private VolunteerUserAvailability mapToVolunteerUserAvailability(VolunteerUserAvailabilityRequest request,
            User user) {
        return VolunteerUserAvailability.builder()
                .user(user)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .lastUpdateDate(request.lastUpdateDate()) 
                .build();
    }

    public Long getNotificationsCountAfterLastAccessed(String userId){
        Optional<LocalDateTime> lastAccessedTimeOptional = userNotificationStatusRepository.findLastAccessedTimeByUser(userId);
        LocalDateTime lastAccessedTime = lastAccessedTimeOptional
                .orElse(LocalDateTime.of(1970, 1, 1, 0, 0));
        Long notificationsCount = notificationRepository.countByUser_IdAndCreatedAtAfter(userId, lastAccessedTime);
        return notificationsCount;
    }


    public void updateUserNewLastAccessedTime(String userId, LocalDateTime newTime){
        UserNotificationStatus userNotificationStatus = userNotificationStatusRepository.findByUserId(userId);
        if (userNotificationStatus == null) {
            // Create new record if it doesn't exist
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()){
                userNotificationStatus = UserNotificationStatus.builder()
                        .user(userOpt.get())
                        .lastAccessedAt(newTime)
                        .build();
            }
            else{
                return;
            }

        } else {
            userNotificationStatus.setLastAccessedAt(newTime);
        }
        userNotificationStatusRepository.save(userNotificationStatus);
    }

    public NotificationPaginationResponse<NotificationsResponse> getNotificationsList(String userId, int page, int size, LocalDateTime clientRefTime){
        LocalDateTime lastAccessedTime;
        LocalDateTime currentAccessedTime;
        Page<Notifications> notificationsPage;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (page == 0 && clientRefTime == null) {
            // First time opening the list: Get time from DB
            Optional<LocalDateTime> lastAccessedTimeOptional = userNotificationStatusRepository.findLastAccessedTimeByUser(userId);
            lastAccessedTime = lastAccessedTimeOptional
                .orElse(LocalDateTime.of(1970, 1, 1, 0, 0));
            currentAccessedTime = LocalDateTime.now();
            updateUserNewLastAccessedTime(userId, currentAccessedTime);
            notificationsPage = notificationRepository.findByUserIdAndCreatedAtLessThanEqual(userId, Optional.of(currentAccessedTime), pageable);

        } else {
            lastAccessedTime = clientRefTime;
            Optional<LocalDateTime> lastAccessedTimeOptional = userNotificationStatusRepository.findLastAccessedTimeByUser(userId);

            notificationsPage = notificationRepository.findByUserIdAndCreatedAtLessThanEqual(userId, lastAccessedTimeOptional, pageable);
        }

        List<NotificationsResponse> list = notificationsPage.stream()
                .map(n -> NotificationsResponse.builder()
                        .type(n.getType().getType_name())
                        .title(n.getType().getDescription())
                        .message(n.getMessage())
                        .date(n.getCreatedAt())
                        .status(n.getCreatedAt().isAfter(lastAccessedTime))
                        .build())
                .collect(Collectors.toList());

        return NotificationPaginationResponse.<NotificationsResponse>builder()
                .items(list)
                .currentPage(notificationsPage.getNumber())
                .totalItems(notificationsPage.getTotalElements())
                .totalPages(notificationsPage.getTotalPages())
                .referenceTimestamp(lastAccessedTime)
                .build();
    }
}
