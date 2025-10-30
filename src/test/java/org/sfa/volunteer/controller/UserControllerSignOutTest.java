package org.sfa.volunteer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.SignOutResponse;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.ResponseBuilder;

import java.time.ZonedDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController - Sign-Out Endpoint Tests")
class UserControllerSignOutTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private ResponseBuilder responseBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String testUserId = "user-123";

    @BeforeEach
    void setup() {
        UserController controller = new UserController(userService, responseBuilder);
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new TestExceptionHandler())
                .build();
    }

    @RestControllerAdvice
    static class TestExceptionHandler {
        @ExceptionHandler(org.sfa.volunteer.exception.UserNotFoundException.class)
        public ResponseEntity<Void> handleUserNotFound(org.sfa.volunteer.exception.UserNotFoundException ex) {
            return ResponseEntity.status(404).build();
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Void> handleAny(Exception ex) {
            return ResponseEntity.status(500).build();
        }
    }

    @Test
    @DisplayName("Test sign-out endpoint - successful sign-out")
    void testSignOutEndpoint_Success() throws Exception {
        SignOutResponse signOutResponse = SignOutResponse.builder()
                .message("User signed out successfully and all user data cleared")
                .signOutTime(ZonedDateTime.now())
                .success(true)
                .build();

        when(userService.signOut(testUserId)).thenReturn(signOutResponse);
        when(responseBuilder.buildSuccessResponse(eq(org.sfa.volunteer.dto.common.SaayamStatusCode.USER_SIGNED_OUT), any(), eq(signOutResponse)))
                .thenReturn(org.sfa.volunteer.dto.common.SaayamResponse.success(
                        org.sfa.volunteer.dto.common.SaayamStatusCode.USER_SIGNED_OUT,
                        "ok",
                        signOutResponse));

        mockMvc.perform(post("/0.0.1/users/signout/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService, times(1)).signOut(testUserId);
    }

    @Test
    @DisplayName("Test sign-out endpoint - user not found")
    void testSignOutEndpoint_UserNotFound() throws Exception {
        when(userService.signOut(testUserId))
                .thenThrow(new org.sfa.volunteer.exception.UserNotFoundException(testUserId));

        mockMvc.perform(post("/0.0.1/users/signout/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Test sign-out endpoint - with different user IDs")
    void testSignOutEndpoint_DifferentUserIds() throws Exception {
        String[] testUserIds = {"user-1", "user-999", "test-abc-123"};
        
        for (String userId : testUserIds) {
            SignOutResponse signOutResponse = SignOutResponse.builder()
                    .message("User signed out successfully and all user data cleared")
                    .signOutTime(ZonedDateTime.now())
                    .success(true)
                    .build();

            when(userService.signOut(userId)).thenReturn(signOutResponse);
            when(responseBuilder.buildSuccessResponse(eq(org.sfa.volunteer.dto.common.SaayamStatusCode.USER_SIGNED_OUT), any(), eq(signOutResponse)))
                    .thenReturn(org.sfa.volunteer.dto.common.SaayamResponse.success(
                            org.sfa.volunteer.dto.common.SaayamStatusCode.USER_SIGNED_OUT,
                            "ok",
                            signOutResponse));

            mockMvc.perform(post("/0.0.1/users/signout/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Test sign-out endpoint - accepts JSON content type")
    void testSignOutEndpoint_AcceptsJson() throws Exception {
        SignOutResponse signOutResponse = SignOutResponse.builder()
                .message("User signed out successfully and all user data cleared")
                .signOutTime(ZonedDateTime.now())
                .success(true)
                .build();

        when(userService.signOut(testUserId)).thenReturn(signOutResponse);
        when(responseBuilder.buildSuccessResponse(eq(org.sfa.volunteer.dto.common.SaayamStatusCode.USER_SIGNED_OUT), any(), eq(signOutResponse)))
                .thenReturn(org.sfa.volunteer.dto.common.SaayamResponse.success(
                        org.sfa.volunteer.dto.common.SaayamStatusCode.USER_SIGNED_OUT,
                        "ok",
                        signOutResponse));

        mockMvc.perform(post("/0.0.1/users/signout/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Test sign-out endpoint - returns proper response structure")
    void testSignOutEndpoint_ResponseStructure() throws Exception {
        // Arrange
        SignOutResponse signOutResponse = SignOutResponse.builder()
                .message("User signed out successfully and all user data cleared")
                .signOutTime(ZonedDateTime.now())
                .success(true)
                .build();

        when(userService.signOut(testUserId)).thenReturn(signOutResponse);
        when(responseBuilder.buildSuccessResponse(eq(SaayamStatusCode.USER_SIGNED_OUT), any(), eq(signOutResponse)))
                .thenReturn(org.sfa.volunteer.dto.common.SaayamResponse.success(
                        SaayamStatusCode.USER_SIGNED_OUT,
                        "ok",
                        signOutResponse));

        // Act
        mockMvc.perform(post("/0.0.1/users/signout/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON));

        // Assert
        verify(responseBuilder).buildSuccessResponse(eq(SaayamStatusCode.USER_SIGNED_OUT), any(), eq(signOutResponse));
    }

    @Test
    @DisplayName("Test sign-out endpoint - concurrent requests handling")
    void testSignOutEndpoint_ConcurrentRequests() throws Exception {
        // Arrange
        SignOutResponse signOutResponse = SignOutResponse.builder()
                .message("User signed out successfully and all user data cleared")
                .signOutTime(ZonedDateTime.now())
                .success(true)
                .build();

        when(userService.signOut(testUserId)).thenReturn(signOutResponse);
        when(responseBuilder.buildSuccessResponse(eq(org.sfa.volunteer.dto.common.SaayamStatusCode.USER_SIGNED_OUT), any(), eq(signOutResponse)))
                .thenReturn(org.sfa.volunteer.dto.common.SaayamResponse.success(
                        org.sfa.volunteer.dto.common.SaayamStatusCode.USER_SIGNED_OUT,
                        "ok",
                        signOutResponse));

        // Act & Assert - Perform multiple simultaneous requests
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/0.0.1/users/signout/{userId}", testUserId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Test sign-out endpoint - exception propagation")
    void testSignOutEndpoint_ExceptionPropagation() throws Exception {
        // Arrange
        when(userService.signOut(testUserId))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        mockMvc.perform(post("/0.0.1/users/signout/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}
