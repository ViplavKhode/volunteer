package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sfa.volunteer.config.SpringContext;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.exception.InvalidRequestException;
import org.sfa.volunteer.exception.EnumUnspecifiedException;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

class UpdateVolunteerStep1HandlerTest {

    private VolunteerService volunteerService;
    private ResponseBuilder responseBuilder;
    private MessageSource messageSource;
    private ObjectMapper objectMapper;
    private Context lambdaContext;
    private UpdateVolunteerStep1Handler handler;
    private ApplicationContext mockedContext;
    private MockedStatic<SpringContext> mockedStaticContext;

    @BeforeEach
    void setUp() throws Exception {
        volunteerService = mock(VolunteerService.class);
        responseBuilder = mock(ResponseBuilder.class);
        messageSource = mock(MessageSource.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        lambdaContext = mock(Context.class);
        mockedContext = mock(ApplicationContext.class);

        when(mockedContext.getBean(VolunteerService.class)).thenReturn(volunteerService);
        when(mockedContext.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
        when(mockedContext.getBean(MessageSource.class)).thenReturn(messageSource);

        // Mock the static SpringContext.getContext() - keep it open during tests
        mockedStaticContext = Mockito.mockStatic(SpringContext.class);
        mockedStaticContext.when(SpringContext::getContext).thenReturn(mockedContext);

        // Set up BaseRequestHandler's static fields using reflection
        // This must be done BEFORE the handler class is loaded/instantiated
        try {
            Field contextField = BaseRequestHandler.class.getDeclaredField("context");
            removeFinalModifier(contextField);
            contextField.set(null, mockedContext);
        } catch (Exception e) {
            // If reflection fails, continue - SpringContext mock should handle it
        }

        try {
            Field messageSourceField = BaseRequestHandler.class.getDeclaredField("messageSource");
            removeFinalModifier(messageSourceField);
            messageSourceField.set(null, messageSource);
        } catch (Exception e) {
            // If reflection fails, continue - SpringContext mock should handle it
        }

        // Also set LambdaExceptionHandler's static messageSource field
        try {
            Field lambdaMessageSourceField = org.sfa.volunteer.exception.LambdaExceptionHandler.class.getDeclaredField("messageSource");
            removeFinalModifier(lambdaMessageSourceField);
            lambdaMessageSourceField.set(null, messageSource);
        } catch (Exception e) {
            // If reflection fails, continue - SpringContext mock should handle it
            // LambdaExceptionHandler will use SpringContext.getContext() which is mocked
        }

        // Instantiate handler (after setting up mocks)
        handler = new UpdateVolunteerStep1Handler();
        
        // CRITICAL: Re-inject mocks after handler instantiation to ensure they're used
        // The handler's static fields are initialized at class load time, so we need to inject after instantiation
        resetMockInjection();
    }

    /**
     * Helper method to modify final static fields using reflection
     * Works with Java 9+ by using MethodHandles or traditional approach with --add-opens
     */
    private void removeFinalModifier(Field field) throws Exception {
        field.setAccessible(true);
        
        // Try MethodHandles approach (Java 9+)
        try {
            java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.privateLookupIn(
                Field.class, java.lang.invoke.MethodHandles.lookup());
            java.lang.invoke.VarHandle modifiersVarHandle = lookup.findVarHandle(
                Field.class, "modifiers", int.class);
            int modifiers = (int) modifiersVarHandle.get(field);
            modifiersVarHandle.set(field, modifiers & ~java.lang.reflect.Modifier.FINAL);
            return;
        } catch (Exception e) {
            // Fallback to traditional approach if MethodHandles fails
        }
        
        // Fallback: Try traditional approach (works with --add-opens in pom.xml)
        // Note: This may fail in Java 9+ but we'll try anyway
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // If we can't modify modifiers, try to set the field directly anyway
            // Some JVM configurations allow setting final fields with just setAccessible(true)
        }
    }

    /**
     * Helper method to reset mock injection for static fields
     * This ensures the handler uses our mocked service instead of the real one
     */
    private void resetMockInjection() {
        try {
            Field volunteerServiceField = UpdateVolunteerStep1Handler.class.getDeclaredField("volunteerService");
            removeFinalModifier(volunteerServiceField);
            volunteerServiceField.set(null, volunteerService);
            
            // Verify the injection worked
            VolunteerService injectedService = (VolunteerService) volunteerServiceField.get(null);
            if (injectedService != volunteerService) {
                System.err.println("[WARNING] Mock injection verification failed, but continuing...");
            } else {
                System.out.println("[TRACE] Successfully injected volunteerService mock");
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to inject volunteerService mock: " + e.getMessage());
            // Don't print stack trace to reduce noise
        }

        try {
            Field responseBuilderField = UpdateVolunteerStep1Handler.class.getDeclaredField("responseBuilder");
            removeFinalModifier(responseBuilderField);
            responseBuilderField.set(null, responseBuilder);
            System.out.println("[TRACE] Successfully injected responseBuilder mock");
        } catch (Exception e) {
            System.err.println("Warning: Failed to inject responseBuilder mock: " + e.getMessage());
            // Don't print stack trace to reduce noise
        }
    }

    @AfterEach
    void tearDown() {
        if (mockedStaticContext != null) {
            mockedStaticContext.close();
        }
    }

    @Test
    void handleRequest_ShouldReturnCreatedResponse_WhenRequestIsValid() throws Exception {

        System.out.println("\n========== TEST START ==========\n");

        // Arrange
        // Include all required fields for validation
        String requestBody = "{\"step\":1,\"userId\":\"user123\",\"termsAndConditions\":true}";

        System.out.println("[TRACE] Creating APIGatewayProxyRequestEvent");
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHeaders(Collections.singletonMap("Accept-Language", "en"))
                .withBody(requestBody);

        System.out.println("[TRACE] Request Headers: " + requestEvent.getHeaders());
        System.out.println("[TRACE] Request Body: " + requestEvent.getBody());

        // Mocked service response
        VolunteerResponse volunteerResponse = VolunteerResponse.builder()
                .userId("user123")
                .termsAndConditions(true)
                .tcUpdateDate(ZonedDateTime.now())
                .build();

        System.out.println("[TRACE] Expected VolunteerResponse DTO: " + volunteerResponse);

        SaayamResponse<VolunteerResponse> saayamResponse = SaayamResponse.<VolunteerResponse>builder()
                .success(true)
                .statusCode(HttpStatus.CREATED.value())
                .saayamCode(SaayamStatusCode.VOLUNTEER_UPDATED.getCode())
                .message("Volunteer updated successfully")
                .data(volunteerResponse)
                .timestamp(ZonedDateTime.now())
                .build();

        System.out.println("[TRACE] Expected SaayamResponse: " + saayamResponse);

        // Re-inject mocks to ensure they're used (critical: must be done before setting up mock behavior)
        resetMockInjection();
        
        // Reset mock to clear any previous behavior
        reset(volunteerService, responseBuilder);

        // Mock volunteerService to return the volunteerResponse
        when(volunteerService.updateVolunteerStep1(any(VolunteerRequest.class)))
                .thenReturn(volunteerResponse);
        
        System.out.println("[TRACE] Mock setup complete for updateVolunteerStep1");

        // Mock responseBuilder to return SaayamResponse
        when(responseBuilder.buildSuccessResponse(
                eq(SaayamStatusCode.VOLUNTEER_UPDATED),
                any(Object[].class),
                eq(volunteerResponse))
        ).thenReturn(saayamResponse);

        // Act
        System.out.println("\n[TRACE] Calling handler.handleRequest...\n");
        APIGatewayProxyResponseEvent result = handler.handleRequest(requestEvent, lambdaContext);

        System.out.println("[TRACE] Handler returned response event: " + result);
        System.out.println("[TRACE] Response body string: " + result.getBody());

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED.value(), result.getStatusCode());
        assertNotNull(result.getBody());

        SaayamResponse<VolunteerResponse> parsed = objectMapper.readValue(
                result.getBody(),
                new TypeReference<SaayamResponse<VolunteerResponse>>() {}
        );

        System.out.println("[TRACE] Parsed SaayamResponse from JSON: " + parsed);

        assertTrue(parsed.success());
        assertEquals(SaayamStatusCode.VOLUNTEER_UPDATED.getCode(), parsed.saayamCode());
        assertNotNull(parsed.data());
        assertEquals("user123", parsed.data().userId());
        assertTrue(parsed.data().termsAndConditions());

        verify(volunteerService, times(1)).updateVolunteerStep1(any(VolunteerRequest.class));
        verify(responseBuilder, times(1)).buildSuccessResponse(
                eq(SaayamStatusCode.VOLUNTEER_UPDATED),
                any(Object[].class),
                eq(volunteerResponse)
        );

        System.out.println("\n========== TEST END ==========\n");
    }

    @Test
    void handleRequest_ShouldReturnBadRequest_WhenEnumUnspecifiedExceptionOccurs() throws Exception {
        // Arrange
        String requestBody = "{\"step\":1,\"userId\":\"user123\",\"termsAndConditions\":true}";
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHeaders(Collections.singletonMap("Accept-Language", "en"))
                .withBody(requestBody);

        // Re-inject mock to ensure it's used
        resetMockInjection();

        EnumUnspecifiedException exception = new EnumUnspecifiedException("Invalid enum value");
        when(volunteerService.updateVolunteerStep1(any(VolunteerRequest.class))).thenThrow(exception);

        // Act
        APIGatewayProxyResponseEvent result = handler.handleRequest(requestEvent, lambdaContext);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCode());
        assertNotNull(result.getBody());

        SaayamResponse<Void> parsed = objectMapper.readValue(
                result.getBody(),
                new TypeReference<SaayamResponse<Void>>() {}
        );

        assertFalse(parsed.success());
        assertEquals(SaayamStatusCode.ENUM_UNSPECIFIED.getCode(), parsed.saayamCode());
        assertEquals("Invalid enum value", parsed.message());

        verify(volunteerService, times(1)).updateVolunteerStep1(any(VolunteerRequest.class));
        verify(responseBuilder, never()).buildSuccessResponse(any(), any(), any());
    }

    @Test
    void handleRequest_ShouldReturnBadRequest_WhenInvalidRequestExceptionOccurs() throws Exception {
        // Arrange
        //String requestBody = "{\"step\":1,\"userId\":\"user123\",\"termsAndConditions\":true}";

        String requestBody = "{\"step\":1,\"userId\":\"user123\",\"termsAndConditions\":true,\"tcUpdateDate\":\"2025-12-09T00:00:00Z\"}";

        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHeaders(Collections.singletonMap("Accept-Language", "en"))
                .withBody(requestBody);

        // Re-inject mock to ensure it's used
        resetMockInjection();

        InvalidRequestException exception = new InvalidRequestException("Invalid request data");
        when(volunteerService.updateVolunteerStep1(any(VolunteerRequest.class))).thenThrow(exception);

        // Act
        APIGatewayProxyResponseEvent result = handler.handleRequest(requestEvent, lambdaContext);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCode());
        assertNotNull(result.getBody());

        SaayamResponse<Void> parsed = objectMapper.readValue(
                result.getBody(),
                new TypeReference<SaayamResponse<Void>>() {}
        );

        assertFalse(parsed.success());
        assertEquals(SaayamStatusCode.BAD_REQUEST.getCode(), parsed.saayamCode());
        assertEquals("Invalid request data", parsed.message());

        verify(volunteerService, times(1)).updateVolunteerStep1(any(VolunteerRequest.class));
        verify(responseBuilder, never()).buildSuccessResponse(any(), any(), any());
    }

    @Test
    void handleRequest_ShouldHandleGeneralException() throws Exception {
        // Arrange
        String requestBody = "{\"step\":1,\"userId\":\"user123\",\"termsAndConditions\":true}";
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHeaders(Collections.singletonMap("Accept-Language", "en"))
                .withBody(requestBody);

        // Re-inject mock to ensure it's used
        resetMockInjection();

        RuntimeException exception = new RuntimeException("Unexpected error");
        when(volunteerService.updateVolunteerStep1(any(VolunteerRequest.class))).thenThrow(exception);

        // Mock LambdaExceptionHandler behavior
        when(messageSource.getMessage(eq("error.general"), any(Object[].class), any())).thenReturn("General error occurred");

        // Act
        APIGatewayProxyResponseEvent result = handler.handleRequest(requestEvent, lambdaContext);

        // Assert
        assertNotNull(result);
        // LambdaExceptionHandler should handle this and return an error response
        assertTrue(result.getStatusCode() >= 400);

        verify(volunteerService, times(1)).updateVolunteerStep1(any(VolunteerRequest.class));
        verify(responseBuilder, never()).buildSuccessResponse(any(), any(), any());
    }

    @Test
    void handleRequest_ShouldHandleInvalidJsonBody() throws Exception {
        // Arrange
        String invalidJsonBody = "{invalid json}";
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHeaders(Collections.singletonMap("Accept-Language", "en"))
                .withBody(invalidJsonBody);

        when(messageSource.getMessage(eq("error.general"), any(Object[].class), any())).thenReturn("General error occurred");

        // Act
        APIGatewayProxyResponseEvent result = handler.handleRequest(requestEvent, lambdaContext);

        // Assert
        assertNotNull(result);
        // Should return an error response due to JSON parsing failure
        assertTrue(result.getStatusCode() >= 400);

        verify(volunteerService, never()).updateVolunteerStep1(any(VolunteerRequest.class));
        verify(responseBuilder, never()).buildSuccessResponse(any(), any(), any());
    }

    @Test
    void handleRequest_ShouldHandleNullBody() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHeaders(Collections.singletonMap("Accept-Language", "en"))
                .withBody(null);

        when(messageSource.getMessage(eq("error.general"), any(Object[].class), any())).thenReturn("General error occurred");

        // Act
        APIGatewayProxyResponseEvent result = handler.handleRequest(requestEvent, lambdaContext);

        // Assert
        assertNotNull(result);
        // Should return an error response due to null body
        assertTrue(result.getStatusCode() >= 400);

        verify(volunteerService, never()).updateVolunteerStep1(any(VolunteerRequest.class));
        verify(responseBuilder, never()).buildSuccessResponse(any(), any(), any());
    }

    @Test
    void handleRequest_ShouldHandleEmptyBody() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHeaders(Collections.singletonMap("Accept-Language", "en"))
                .withBody("");

        when(messageSource.getMessage(eq("error.general"), any(Object[].class), any())).thenReturn("General error occurred");

        // Act
        APIGatewayProxyResponseEvent result = handler.handleRequest(requestEvent, lambdaContext);

        // Assert
        assertNotNull(result);
        // Should return an error response due to empty body
        assertTrue(result.getStatusCode() >= 400);

        verify(volunteerService, never()).updateVolunteerStep1(any(VolunteerRequest.class));
        verify(responseBuilder, never()).buildSuccessResponse(any(), any(), any());
    }

    @Test
    void handleRequest_ShouldHandleMissingRequiredFields() throws Exception {
        // Arrange
        String requestBody = "{\"step\":1}"; // Missing userId
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHeaders(Collections.singletonMap("Accept-Language", "en"))
                .withBody(requestBody);

        // Re-inject mock to ensure it's used
        resetMockInjection();
        
        // The handler calls the service, which should throw InvalidRequestException for missing fields
        InvalidRequestException exception = new InvalidRequestException("Missing required field: userId");
        when(volunteerService.updateVolunteerStep1(any(VolunteerRequest.class))).thenThrow(exception);

        // Act
        APIGatewayProxyResponseEvent result = handler.handleRequest(requestEvent, lambdaContext);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCode());
        assertNotNull(result.getBody());

        SaayamResponse<Void> parsed = objectMapper.readValue(
                result.getBody(),
                new TypeReference<SaayamResponse<Void>>() {}
        );

        assertFalse(parsed.success());
        assertEquals(SaayamStatusCode.BAD_REQUEST.getCode(), parsed.saayamCode());

        verify(volunteerService, times(1)).updateVolunteerStep1(any(VolunteerRequest.class));
        verify(responseBuilder, never()).buildSuccessResponse(any(), any(), any());
    }

    @Test
    void handleRequest_ShouldHandleDifferentLocales() throws Exception {
        // Arrange - Test with different locale headers
        String requestBody = "{\"step\":1,\"userId\":\"user123\",\"termsAndConditions\":true}";

        Map<String, String> locales = Map.of(
                "en", "en",
                "fr-FR", "fr",
                "zh-CN", "zh",
                "hi-IN", "hi"
        );

        for (Map.Entry<String, String> localeEntry : locales.entrySet()) {
            // Re-inject mock to ensure it's used
            resetMockInjection();
            
            APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                    .withHeaders(Collections.singletonMap("Accept-Language", localeEntry.getKey()))
                    .withBody(requestBody);

            VolunteerResponse volunteerResponse = VolunteerResponse.builder()
                    .userId("user123")
                    .termsAndConditions(true)
                    .tcUpdateDate(ZonedDateTime.now())
                    .build();

            SaayamResponse<VolunteerResponse> saayamResponse = SaayamResponse.<VolunteerResponse>builder()
                    .success(true)
                    .statusCode(HttpStatus.CREATED.value())
                    .saayamCode(SaayamStatusCode.VOLUNTEER_UPDATED.getCode())
                    .message("Volunteer updated successfully")
                    .data(volunteerResponse)
                    .timestamp(ZonedDateTime.now())
                    .build();

            when(volunteerService.updateVolunteerStep1(any(VolunteerRequest.class))).thenReturn(volunteerResponse);
            when(responseBuilder.buildSuccessResponse(
                    eq(SaayamStatusCode.VOLUNTEER_UPDATED),
                    any(Object[].class),
                    eq(volunteerResponse))
            ).thenReturn(saayamResponse);

            // Act
            APIGatewayProxyResponseEvent result = handler.handleRequest(requestEvent, lambdaContext);

            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.CREATED.value(), result.getStatusCode());
            assertNotNull(result.getBody());

            verify(volunteerService, atLeastOnce()).updateVolunteerStep1(any(VolunteerRequest.class));
        }
    }

    @Test
    void handleRequest_ShouldParseRequestCorrectly() throws Exception {
        // Arrange
        String requestBody = "{\"step\":1,\"userId\":\"user123\",\"termsAndConditions\":true,\"notification\":false}";
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHeaders(Collections.singletonMap("Accept-Language", "en"))
                .withBody(requestBody);

        // Re-inject mock to ensure it's used
        resetMockInjection();

        VolunteerResponse volunteerResponse = VolunteerResponse.builder()
                .userId("user123")
                .termsAndConditions(true)
                .notification(false)
                .tcUpdateDate(ZonedDateTime.now())
                .build();

        SaayamResponse<VolunteerResponse> saayamResponse = SaayamResponse.<VolunteerResponse>builder()
                .success(true)
                .statusCode(HttpStatus.CREATED.value())
                .saayamCode(SaayamStatusCode.VOLUNTEER_UPDATED.getCode())
                .message("Volunteer updated successfully")
                .data(volunteerResponse)
                .timestamp(ZonedDateTime.now())
                .build();

        when(volunteerService.updateVolunteerStep1(any(VolunteerRequest.class))).thenAnswer(invocation -> {
            VolunteerRequest request = invocation.getArgument(0);
            assertEquals(1, request.step());
            assertEquals("user123", request.userId());
            assertEquals(true, request.termsAndConditions());
            assertEquals(false, request.notification());
            return volunteerResponse;
        });
        when(responseBuilder.buildSuccessResponse(
                eq(SaayamStatusCode.VOLUNTEER_UPDATED),
                any(Object[].class),
                eq(volunteerResponse))
        ).thenReturn(saayamResponse);

        // Act
        APIGatewayProxyResponseEvent result = handler.handleRequest(requestEvent, lambdaContext);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED.value(), result.getStatusCode());
        verify(volunteerService, times(1)).updateVolunteerStep1(any(VolunteerRequest.class));
    }

    @Test
    void handleRequest_ShouldHandleMalformedJsonGracefully() throws Exception {
        // Arrange
        String[] malformedJsonBodies = {
                "{step:1}",  // Missing quotes
                "{\"step\":}",  // Missing value
                "{'step':1}",  // Single quotes
                "step=1",  // Not JSON
                "{\"step\":1,\"userId\":}",  // Incomplete
        };

        when(messageSource.getMessage(eq("error.general"), any(Object[].class), any())).thenReturn("General error occurred");

        for (String malformedBody : malformedJsonBodies) {
            APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                    .withHeaders(Collections.singletonMap("Accept-Language", "en"))
                    .withBody(malformedBody);

            // Act
            APIGatewayProxyResponseEvent result = handler.handleRequest(requestEvent, lambdaContext);

            // Assert
            assertNotNull(result);
            assertTrue(result.getStatusCode() >= 400, "Should return error for malformed JSON: " + malformedBody);
            verify(volunteerService, never()).updateVolunteerStep1(any(VolunteerRequest.class));
        }
    }
}

