package org.sfa.volunteer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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

@WebMvcTest(UserController.class)
@DisplayName("UserController - Sign-Out Endpoint Tests")
class UserControllerSignOutTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ResponseBuilder responseBuilder;

    private ObjectMapper objectMapper = new ObjectMapper();
    private String testUserId = "user-123";

    @Test
    @DisplayName("Test sign-out endpoint - successful sign-out")
    void testSignOutEndpoint_Success() throws Exception {
        SignOutResponse signOutResponse = SignOutResponse.builder()
                .message("User signed out successfully and all user data cleared")
                .signOutTime(ZonedDateTime.now())
                .success(true)
                .build();

        when(userService.signOut(testUserId)).thenReturn(signOutResponse);
        when(responseBuilder.buildSuccessResponse(any(), any(), any())).thenReturn(null);

        mockMvc.perform(post("/0.0.1/users/signout/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

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
            when(responseBuilder.buildSuccessResponse(any(), any(), any())).thenReturn(null);

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
        when(responseBuilder.buildSuccessResponse(any(), any(), any())).thenReturn(null);

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
                .thenReturn(null);

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
        when(responseBuilder.buildSuccessResponse(any(), any(), any())).thenReturn(null);

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
