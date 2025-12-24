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
    private String regionHint(HttpServletRequest req) {
        String r = req.getHeader(HDR_REGION);
        return (r == null || r.isBlank()) ? "us-east-1" : r;
    }
    // 1) GET view URL for a given userId
    @GetMapping("/{userId}/profile-image")
    public ResponseEntity<?> getProfileImage(@PathVariable String userId, HttpServletRequest req) {
        var url = profileImageStorageService.presignView(userId, regionHint(req));
        return url.<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of("userId", userId, "url", u.toString())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
    // 2) Generate upload URL
    @PostMapping("/{userId}/profile-image/upload-url")
    public SaayamResponse<Map<String, Object>> createUploadUrl(
            @PathVariable String userId,
            @RequestParam("contentType") String contentType,
            @RequestParam("contentLength") long contentLength,
            HttpServletRequest req) {

        var payload = profileImageStorageService.presignUpload(userId, contentType, contentLength, regionHint(req));
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, payload);
    }
    // Confirm upload (save path to DB)
    @PostMapping("/{userId}/profile-image/confirm")
    public SaayamResponse<Map<String, String>> confirmUpload(
            @PathVariable String userId,
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {

        String s3Uri = body.get("s3Uri"); // we will return this from upload-url step
        profileImageStorageService.confirmUpload(userId, s3Uri);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS,
                Map.of("userId", userId, "message", "Profile image saved"));
    }
    // 3) DELETE image for a given userId
    @DeleteMapping("/{userId}/profile-image")
    public SaayamResponse<Map<String, String>> deleteProfileImage(@PathVariable String userId, HttpServletRequest req) {
        profileImageStorageService.delete(userId, regionHint(req));
        return responseBuilder.buildSuccessResponse(
                SaayamStatusCode.SUCCESS, Map.of("userId", userId, "message", "Profile image deleted"));
    }
    @DeleteMapping("/profile/signoff/{userId}")
    public SaayamResponse<SignOffResponse> signOffUser(
            @PathVariable String userId,
            @RequestBody(required = false) SignOffRequest request) {
        String reason = (request != null ? request.reason() : null);
        profileImageStorageService.delete(userId, "us-east-1");
        SignOffResponse response = userService.signOffUser(userId, reason);
        return responseBuilder.buildSuccessResponse(
                SaayamStatusCode.USER_DELETED,
                new Object[]{userId},
                response
        );
    }
}
