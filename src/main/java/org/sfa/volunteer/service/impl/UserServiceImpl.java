package org.sfa.volunteer.service.impl;

import jakarta.transaction.Transactional;
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdateOrganizationRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.response.*;
import org.sfa.volunteer.exception.UserCategoryNotFoundException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.exception.UserOrganizationNotFoundException;
import org.sfa.volunteer.model.*;
import org.sfa.volunteer.repository.*;
import org.sfa.volunteer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
// Profile Pic Upload.
import org.springframework.core.env.Environment;
import org.apache.commons.io.FilenameUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;
    private final OrganizationRepository organizationRepository;

    private final UserCategoryRepository userCategoryRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    // Default IDs for user status and category
    private static final Integer DEFAULT_USER_STATUS_ID = 1; // Active user
    private static final Integer DEFAULT_USER_CATEGORY_ID = 1; // User Category: common user
    private static final Integer VOLUNTEER_CATEGORY_ID = 2; // User Category: volunteer
    // Profile Pic Upload
    private final S3Presigner s3Presigner;
    private final Environment env;
    private final S3Client s3Client;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserStatusRepository userStatusRepository, OrganizationRepository organizationRepository, UserCategoryRepository userCategoryRepository,
                           CountryRepository countryRepository, StateRepository stateRepository, S3Presigner s3Presigner, Environment env, S3Client s3Client) {
        this.userRepository = userRepository;
        this.userStatusRepository = userStatusRepository;
        this.organizationRepository = organizationRepository;
        this.userCategoryRepository = userCategoryRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.s3Presigner = s3Presigner;
        this.env = env;
        this.s3Client = s3Client;
    }

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {

        UserStatus userStatus = userStatusRepository.findById(DEFAULT_USER_STATUS_ID)
                .orElseThrow(() -> new UserCategoryNotFoundException(DEFAULT_USER_STATUS_ID));

        UserCategory userCategory = userCategoryRepository.findById(DEFAULT_USER_CATEGORY_ID)
                .orElseThrow(() -> new UserCategoryNotFoundException(DEFAULT_USER_CATEGORY_ID));

        Country country = countryRepository.findByCountryName(request.country())
                .orElseThrow(() -> new UserCategoryNotFoundException(DEFAULT_USER_CATEGORY_ID));

        // Create a new User entity from the request data
        User user = User.builder()
                .fullName(request.name())
                .primaryEmailAddress(request.email())
                .primaryPhoneNumber(request.phoneNumber())
                .timeZone(request.timeZone())
                .lastUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")))
                .userCategory(userCategory)
                .userStatus(userStatus)
                .country(country)
                .build();

        // Save the User entity to the database
        user = userRepository.save(user);

        // Create a response object from the saved User entity
        return CreateUserResponse.builder()
                .name(user.getFullName())
                .email(user.getPrimaryEmailAddress())
                .phoneNumber(user.getPrimaryEmailAddress())
                .timeZone(user.getTimeZone())
                .userId(user.getId())
                .countryName(user.getCountry() != null ? user.getCountry().getCountryName() : null)
                .build();
    }

    @Override
    public UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Update all fields from the request without null checks
        user.setFirstName(request.firstName());
        user.setMiddleName(request.middleName());
        user.setLastName(request.lastName());
        user.setAddressLine1(request.addressLine1());
        user.setAddressLine2(request.addressLine2());
        user.setAddressLine3(request.addressLine3());
        user.setCity(request.cityName());
        user.setZipCode(request.zipCode());
        user.setProfilePicturePath(request.profilePicturePath());
        user.setVolunteerStage(request.volunteerStage());
        user.setVolunteerUpdateDate(request.volunteerUpdateDate());
        user.setLastUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));

        User updatedUser = userRepository.save(user);

        return mapToUserProfileResponse(updatedUser);
    }

    @Override
    public PaginationResponse<UserProfileResponse> findAllUsersWithPagination(Integer pageNumber, Integer pageSize) {
        int pageNum = (pageNumber == null) ? DEFAULT_PAGE : pageNumber;
        int pageSizeNum = (pageSize == null) ? DEFAULT_SIZE : pageSize;
        Pageable pageable = PageRequest.of(pageNum, pageSizeNum);
        Page<User> userPage = userRepository.findAll(pageable);

        List<UserProfileResponse> userProfiles = userPage.stream()
                .map(this::mapToUserProfileResponse)
                .collect(Collectors.toList());

        return PaginationResponse.<UserProfileResponse>builder()
                .currentPage(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalPages(userPage.getTotalPages())
                .totalItems(userPage.getTotalElements())
                .items(userProfiles)
                .hasNextPage(userPage.hasNext())
                .hasPreviousPage(userPage.hasPrevious())
                .build();
    }

    @Override
    public UserProfileResponse getUserProfileById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return mapToUserProfileResponse(user);
    }

    @Override
    public WizardStatusResponse getWizardStatus(String userId) {
    UserProfileResponse userProfile = getUserProfileById(userId);

    String addressAvailable = (userProfile.addressLine1() != null && !userProfile.addressLine1().trim().isEmpty())
            ? "Y" : "N";

    return new WizardStatusResponse(
        userId,
        userProfile.promotionWizardStage(),
        addressAvailable
    );
}
    
    @Override
    public AddressStatusResponse getAddressStatus(String userId) {
    UserProfileResponse userProfile = getUserProfileById(userId);

    String addressAvailable = (userProfile.addressLine1() != null && !userProfile.addressLine1().trim().isEmpty())
            ? "Y" : "N";

    return new AddressStatusResponse(
        userId,
        addressAvailable
    );
}




    @Override
    public UserProfileResponse getUserProfileByEmail(String email) {
        List<User> user = userRepository.findByPrimaryEmailAddress(email);

        if (user.isEmpty()) {
            throw new UserNotFoundException(email);
        }

        return mapToUserProfileResponse(user.get(0));
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .emailAddress(user.getPrimaryEmailAddress())
                .phoneNumber(user.getPrimaryPhoneNumber())
                .timeZone(user.getTimeZone())
                .profilePicturePath(user.getProfilePicturePath())
                .addressLine1(user.getAddressLine1())
                .addressLine2(user.getAddressLine2())
                .addressLine3(user.getAddressLine3())
                .city(user.getCity())
                .zipCode(user.getZipCode())
                .lastUpdateDate(user.getLastUpdateDate())
                .stateName(user.getState() != null ? user.getState().getStateName() : null)
                .countryName(user.getCountry() != null ? user.getCountry().getCountryName() : null)
                .userStatus(user.getUserStatus() != null ? user.getUserStatus().getUserStatus() : null)
                .userCategory(user.getUserCategory() != null ? user.getUserCategory().getUserCategory() : null)
                .gender(user.getGender())
                .lastLocation(user.getLastLocation())
                .language1(user.getLanguage1())
                .language2(user.getLanguage2())
                .language3(user.getLanguage3())
                .promotionWizardStage(user.getVolunteerStage())
                .promotionWizardLastUpdateDate(user.getVolunteerUpdateDate())
                .build();
    }

    @Override
    public OrganizationResponse updateUserOrganization(String userId, UpdateOrganizationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Organization organization = organizationRepository.findByUser(user).orElse(null);

        if (organization == null) {
            organization = new Organization();
            organization.setUser(user);
        }

        if (request.organizationName() != null) organization.setOrganizationName(request.organizationName());
        if (request.organizationType() != null) organization.setOrganizationType(request.organizationType());
        if (request.phoneNumber() != null) organization.setPhoneNumber(request.phoneNumber());
        if (request.email() != null) organization.setEmail(request.email());
        if (request.url() != null) organization.setUrl(request.url());
        if (request.streetAddress1() != null) organization.setStreetAddress1(request.streetAddress1());
        if (request.streetAddress2() != null) organization.setStreetAddress2(request.streetAddress2());
        if (request.city() != null) organization.setCity(request.city());
        if (request.state() != null) organization.setState(request.state());
        if (request.zipCode() != null) organization.setZipCode(request.zipCode());

        organization.setLastUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));
        Organization updatedOrganization = organizationRepository.save(organization);

        return OrganizationResponse.builder()
                .id(updatedOrganization.getId())
                .organizationName(updatedOrganization.getOrganizationName())
                .organizationType(updatedOrganization.getOrganizationType())
                .phoneNumber(updatedOrganization.getPhoneNumber())
                .email(updatedOrganization.getEmail())
                .url(updatedOrganization.getUrl())
                .streetAddress1(updatedOrganization.getStreetAddress1())
                .streetAddress2(updatedOrganization.getStreetAddress2())
                .city(updatedOrganization.getCity())
                .state(updatedOrganization.getState())
                .zipCode(updatedOrganization.getZipCode())
                .build();
    }

    @Override
    public OrganizationResponse getOrganizationByUserId(String userId) {
        Organization organization = organizationRepository.findByUserId(userId)
                .orElseThrow(() -> new UserOrganizationNotFoundException(userId));

        return OrganizationResponse.builder()
                .id(organization.getId())
                .organizationName(organization.getOrganizationName())
                .organizationName(organization.getOrganizationName())
                .organizationType(organization.getOrganizationType())
                .phoneNumber(organization.getPhoneNumber())
                .email(organization.getEmail())
                .url(organization.getUrl())
                .streetAddress1(organization.getStreetAddress1())
                .streetAddress2(organization.getStreetAddress2())
                .city(organization.getCity())
                .state(organization.getState())
                .zipCode(organization.getZipCode())
                .build();
    }
    // Profile Pic Upload
    // MinIO POC
    @Deprecated
    @Override
    public String computeProfilePicKey(String userId, String fileName) {
        String ext = java.util.Optional.ofNullable(FilenameUtils.getExtension(fileName)).orElse("jpg").toLowerCase();
        return "profile-pics/" + userId + "/" + java.util.UUID.randomUUID() + "." + ext;
    }

    @Deprecated
    @Override
    public PresignedPutObjectRequest presignProfilePicPut(String key, String contentType) {
        // simple validation to avoid bad types
        if (!( "image/png".equals(contentType) ||
                "image/jpeg".equals(contentType) ||
                "image/webp".equals(contentType))) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }

        final String bucket = env.getProperty("minio.bucket", "saayam-local-private");

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        return s3Presigner.presignPutObject(r -> r
                .putObjectRequest(put)
                .signatureDuration(java.time.Duration.ofMinutes(10)));
    }

    @Deprecated
    @Override
    public PresignedGetObjectRequest presignProfilePicGet(String key) {
        final String bucket = env.getProperty("minio.bucket", "saayam-local-private");

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return s3Presigner.presignGetObject(r -> r
                .getObjectRequest(get)
                .signatureDuration(java.time.Duration.ofMinutes(5)));
    }

    @Deprecated
    @Override
    public void finalizeProfilePic(String userId, String newKey) {
       String oldKey = null;
        if (oldKey != null && !oldKey.equals(newKey)) {
            final String bucket = env.getProperty("minio.bucket", "saayam-local-private");
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(oldKey)
                        .build());
            } catch (Exception ignore) {
            }
        }
    }

    @Deprecated
    @Override
    public void deleteProfilePic(String userId) {
        // 1) load current key
        String key = null;
        if (key == null) {
            return;
        }

        final String bucket = env.getProperty("minio.bucket", "saayam-local-private");
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (Exception ignore) {
            // best-effort
        }
    }
    // S3 URI <-> DB //
    @Override
    public void setProfilePicturePath(String userId, String s3Uri) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setProfilePicturePath(s3Uri);  // store S3 URI here
        user.setLastUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));
        userRepository.save(user);
    }

    @Override
    public Optional<String> getProfilePicturePath(String userId) {
        return userRepository.findById(userId)
                .map(User::getProfilePicturePath)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank());
    }

}
