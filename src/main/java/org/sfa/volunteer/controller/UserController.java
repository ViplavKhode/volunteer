package org.sfa.volunteer.controller;

import jakarta.validation.Valid;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdateOrganizationRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.request.SignOffRequest;
import org.sfa.volunteer.dto.response.AddressStatusResponse;
import org.sfa.volunteer.dto.response.CreateUserResponse;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.SignOffResponse;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.dto.response.WizardStatusResponse;
import org.sfa.volunteer.dto.response.*;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/0.0.1/users")
public class UserController {

    private final UserService userService;
    private final ResponseBuilder responseBuilder;

    private final ProfileImageStorageService profileImageStorageService;
    private static final String HDR_DEV_UID = "X-Dev-UserId";
    private static final String HDR_REGION  = "X-Dev-Region";


    @Autowired
    public UserController(UserService userService, ResponseBuilder responseBuilder, ProfileImageStorageService profileImageStorageService) {
        this.userService = userService;
        this.responseBuilder = responseBuilder;
        this.profileImageStorageService = profileImageStorageService;
    }

    @PostMapping
    public SaayamResponse<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = userService.createUser(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.USER_CREATED, response);
    }

    @GetMapping
    public SaayamResponse<PaginationResponse<UserProfileResponse>> getUsersWithPagination(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        PaginationResponse<UserProfileResponse> response = userService.findAllUsersWithPagination(page, size);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, response);
    }

    @GetMapping("/profile")
    public SaayamResponse<UserProfileResponse> getUserProfileByEmail(@Valid @RequestBody CreateUserRequest request) {
        String email = request.email().toString();
        UserProfileResponse response = userService.getUserProfileByEmail(email);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{email}, response);
    }

    @GetMapping("/profile/{userId}")
    public SaayamResponse<UserProfileResponse> getUserProfile(@PathVariable String userId) {
        UserProfileResponse response = userService.getUserProfileById(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    }

    @GetMapping("/wizard/{userId}")
    public SaayamResponse<WizardStatusResponse> getWizardStatus(@PathVariable String userId) {
        WizardStatusResponse response = userService.getWizardStatus(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    }

    @GetMapping("/addressStatus/{userId}")
    public SaayamResponse<AddressStatusResponse> getAddressStatus(@PathVariable String userId) {
    	AddressStatusResponse response = userService.getAddressStatus(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    } 

    @GetMapping("/login/{email}")
    public SaayamResponse<UserProfileResponse> getUserProfileAfterLogin(@PathVariable String email) {
        UserProfileResponse response = userService.getUserProfileByEmail(email);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{email}, response);
    }

    @PutMapping("/profile/{userId}")
    public SaayamResponse<UserProfileResponse> updateUserProfile(
            @PathVariable String userId,
            @RequestBody UpdateUserProfileRequest request) {
        UserProfileResponse response = userService.updateUserProfile(userId, request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.USER_ACCOUNT_UPDATED, new Object[]{userId}, response);
    }

    @PutMapping("/organization/{userId}")
    public SaayamResponse<OrganizationResponse> updateUserOrganization(
            @PathVariable String userId,
            @RequestBody UpdateOrganizationRequest request) {
        OrganizationResponse response = userService.updateUserOrganization(userId, request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    }

    @GetMapping("/organization/{userId}")
    public SaayamResponse<OrganizationResponse> getOrganizationByUserId(@PathVariable String userId) {
        OrganizationResponse organization = userService.getOrganizationByUserId(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, organization);
    }
    /* Profile Pic Upload */
    // Helper
    private String currentUserId(HttpServletRequest req) {
        // Login Payload
        String override = req.getHeader(HDR_DEV_UID);
        if (override != null && !override.isBlank()) return override;
        return "11111111-1111-1111-1111-111111111111";
    }
    private String regionHint(HttpServletRequest req) {
        String r = req.getHeader(HDR_REGION);
        return (r == null || r.isBlank()) ? "us-east-1" : r;
    }

    @DeleteMapping("/profile/signoff/{userId}")
    public SaayamResponse<SignOffResponse> signOffUser(
            @PathVariable String userId,
            @RequestBody SignOffRequest request) {

        SignOffResponse response = userService.signOffUser(userId, request.reason());

        return responseBuilder.buildSuccessResponse(
                SaayamStatusCode.USER_DELETED,
                new Object[]{userId},
                response
        );
    }
    // 1) GET view URL
    @GetMapping("/me/profile-image")
    public ResponseEntity<?> getProfileImage(HttpServletRequest req) {
        var url = profileImageStorageService.presignView(currentUserId(req), regionHint(req));
        return url.<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of("url", u.toString())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
    // 2) Upload (multipart)
    @PostMapping(value = "/me/profile-image",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public SaayamResponse<Map<String, Object>> uploadProfileImage(
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
            jakarta.servlet.http.HttpServletRequest req) throws java.io.IOException {
        var payload = profileImageStorageService.uploadMultipart(
                currentUserId(req), file, regionHint(req));
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, payload);
    }
    // 3) DELETE image
    @DeleteMapping("/me/profile-image")
    public SaayamResponse<Map<String, String>> deleteProfileImage(HttpServletRequest req) {
        profileImageStorageService.delete(currentUserId(req), regionHint(req));
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, Map.of("message", "Profile image deleted"));
    }
}
