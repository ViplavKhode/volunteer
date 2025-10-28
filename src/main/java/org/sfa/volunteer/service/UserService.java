package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdateOrganizationRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;

import org.sfa.volunteer.dto.response.*;


public interface UserService {

    PaginationResponse<UserProfileResponse> findAllUsersWithPagination(Integer pageNumber, Integer pageSize);

    UserProfileResponse getUserProfileById(String userId);
    
    WizardStatusResponse getWizardStatus(String userId);
    
    AddressStatusResponse getAddressStatus(String userId);

    UserProfileResponse getUserProfileByEmail(String email);

    CreateUserResponse createUser(CreateUserRequest createUserRequest);

    UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest updateUserProfileRequest);

    OrganizationResponse updateUserOrganization(String userId, UpdateOrganizationRequest request);

    OrganizationResponse getOrganizationByUserId(String userId);

    // Profile Pic Upload
    // MinIO POC
    @Deprecated
    software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
    presignProfilePicPut(String key, String contentType);

    @Deprecated
    software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
    presignProfilePicGet(String key);

    @Deprecated
    String computeProfilePicKey(String userId, String fileName);

    // Saves the new key; deletes the old key if it exists.
    @Deprecated
    void finalizeProfilePic(String userId, String newKey);

    // Deletes the current profile picture and clears the saved key.
    @Deprecated
    void deleteProfilePic(String userId);

    // AWS (S3 URI <-> DB)
    // Profile image persistence (reuse profilePicturePath column)
    void setProfilePicturePath(String userId, String s3Uri);
    java.util.Optional<String> getProfilePicturePath(String userId);
}
