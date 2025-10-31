package org.sfa.volunteer.controller;

import jakarta.validation.Valid;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdateOrganizationRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.response.*;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/0.0.1/users")

public class UserController {
    private final UserService userService;
    private final ResponseBuilder responseBuilder;
    private final ProfileImageStorageService profileImageStorageService;

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
    private String currentUserId(HttpServletRequest req) {
        // TODO: User Id from Login Payload -
        String override = req.getHeader("X-Dev-UserId");
        if (override != null && !override.isBlank()) return override;
        return "11111111-1111-1111-1111-111111111111";
    }

    private String regionHint(HttpServletRequest req) {
        String r = req.getHeader("X-Dev-Region");
        return (r == null || r.isBlank()) ? "us-east-1" : r;
    }

    /* . */
    @GetMapping("/me/bootstrap")
    public SaayamResponse<Map<String, Object>> bootstrapMe(HttpServletRequest req) {
        String email = req.getHeader("X-Auth-Email");
        String userIdOverride = req.getHeader("X-Dev-UserId");
        String region = req.getHeader("X-Dev-Region"); // "eu-west-1" | "us-east-1"

        UserProfileResponse user;
        if (email != null && !email.isBlank()) {
            user = userService.getUserProfileByEmail(email);
        } else if (userIdOverride != null && !userIdOverride.isBlank()) {
            user = userService.getUserProfileById(userIdOverride);
        } else {
            return responseBuilder.buildErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    SaayamStatusCode.BAD_REQUEST,
                    "Missing identity (X-Auth-Email or X-Dev-UserId)"
            );
        }

        String uid = user.id();
        var presignedOpt = profileImageStorageService.presignView(uid, region);

        // Use a map that allows nulls
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("userId", uid);
        payload.put("fullName", user.fullName());          // may be null → allowed
        payload.put("email", user.emailAddress());         // may be null → allowed
        payload.put("hasImage", presignedOpt.isPresent());
        payload.put("profileImageUrl", presignedOpt.map(Object::toString).orElse(null));

        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, payload);
    }

    // 1) GET view URL (204 if none)
    @GetMapping("/me/profile-image")
    public ResponseEntity<?> getProfileImage(HttpServletRequest req) {
        var url = profileImageStorageService.presignView(currentUserId(req), regionHint(req));
        return url.<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of("url", u.toString())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // 2) POST presign upload
    @PostMapping("/me/profile-image/presign")
    public Map<String,Object> presignProfileImage(@RequestBody Map<String,Object> body, HttpServletRequest req) {
        String mime = (String) body.get("mimeType");
        long size = ((Number) body.get("size")).longValue();
        var up = profileImageStorageService.presignUpload(currentUserId(req), mime, size, regionHint(req));
        return Map.of("url", up.url().toString(), "key", up.key(), "headers", up.headers());
    }

    // 3) POST confirm
    @PostMapping("/me/profile-image/confirm")
    public SaayamResponse<Map<String, String>> confirmProfileImage(@RequestBody Map<String, String> body,
                                                                   HttpServletRequest req) {
        String key  = body.get("key");
        String etag = body.getOrDefault("etag", "");
        profileImageStorageService.confirmUpload(currentUserId(req), key, etag, regionHint(req));

        Map<String, String> payload = Map.of(
                "message", "Profile image saved",
                "key", key
        );
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, payload);
    }

    // 4) DELETE image
    @DeleteMapping("/me/profile-image")
    public SaayamResponse<Map<String, String>> deleteProfileImage(HttpServletRequest req) {
        profileImageStorageService.delete(currentUserId(req), regionHint(req));
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, Map.of("message", "Profile image deleted"));
    }

    // new API - Upload
    @PostMapping(value = "/me/profile-image",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public SaayamResponse<Map<String, Object>> uploadProfileImage(
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
            jakarta.servlet.http.HttpServletRequest req) throws java.io.IOException {
        var payload = profileImageStorageService.uploadMultipart(
                currentUserId(req), file, regionHint(req));
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, payload);
    }

}
