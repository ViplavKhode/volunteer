package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sfa.volunteer.config.SpringContext;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for BaseRequestHandler.
 * Since BaseRequestHandler is abstract, we test it through a concrete implementation
 * or by testing its protected methods via reflection or a test subclass.
 */
class BaseRequestHandlerTest {

    private MessageSource messageSource;
    private ObjectMapper objectMapper;
    private ApplicationContext mockedContext;
    private MockedStatic<SpringContext> mockedStaticContext;
    private TestConcreteHandler handler;

    /**
     * Concrete test implementation of BaseRequestHandler for testing purposes
     */
    private static class TestConcreteHandler extends BaseRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        @Override
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            // Simple implementation for testing base class methods
            return createResponse(HttpStatus.OK.value(), Map.of("message", "test"));
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        messageSource = mock(MessageSource.class);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mockedContext = mock(ApplicationContext.class);

        when(mockedContext.getBean(MessageSource.class)).thenReturn(messageSource);

        // Mock the static SpringContext.getContext()
        mockedStaticContext = Mockito.mockStatic(SpringContext.class);
        mockedStaticContext.when(SpringContext::getContext).thenReturn(mockedContext);

        // Use reflection to inject mocks into static fields
        // Note: We need to remove the 'final' modifier temporarily to set static final fields
        try {
            Field contextField = BaseRequestHandler.class.getDeclaredField("context");
            contextField.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(contextField, contextField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            contextField.set(null, mockedContext);
        } catch (Exception e) {
            // If reflection fails, continue - SpringContext mock should handle it
        }

        try {
            Field messageSourceField = BaseRequestHandler.class.getDeclaredField("messageSource");
            messageSourceField.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(messageSourceField, messageSourceField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            messageSourceField.set(null, messageSource);
        } catch (Exception e) {
            // If reflection fails, continue - SpringContext mock should handle it
        }

        try {
            Field objectMapperField = BaseRequestHandler.class.getDeclaredField("objectMapper");
            objectMapperField.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(objectMapperField, objectMapperField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            objectMapperField.set(null, objectMapper);
        } catch (Exception e) {
            // If reflection fails, continue - ObjectMapperConfig mock should handle it
        }

        handler = new TestConcreteHandler();
    }

    @AfterEach
    void tearDown() {
        if (mockedStaticContext != null) {
            mockedStaticContext.close();
        }
    }

    @Test
    void getLocaleFromRequest_ShouldReturnLocaleFromHeaders_WhenAcceptLanguageHeaderExists() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHeaders(Collections.singletonMap("Accept-Language", "fr-FR"));

        // Act - Use reflection to access protected method
        java.lang.reflect.Method method = BaseRequestHandler.class.getDeclaredMethod(
                "getLocaleFromRequest", APIGatewayProxyRequestEvent.class);
        method.setAccessible(true);
        Locale result = (Locale) method.invoke(handler, requestEvent);

        // Assert
        assertNotNull(result);
        assertEquals("fr", result.getLanguage());
        assertEquals("FR", result.getCountry());
    }

    @Test
    void getLocaleFromRequest_ShouldReturnDefaultLocale_WhenAcceptLanguageHeaderIsMissing() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHeaders(Collections.emptyMap());

        // Act
        java.lang.reflect.Method method = BaseRequestHandler.class.getDeclaredMethod(
                "getLocaleFromRequest", APIGatewayProxyRequestEvent.class);
        method.setAccessible(true);
        Locale result = (Locale) method.invoke(handler, requestEvent);

        // Assert
        assertNotNull(result);
        assertEquals("en", result.getLanguage());
        assertEquals("US", result.getCountry());
    }

    @Test
    void getLocaleFromRequest_ShouldReturnDefaultLocale_WhenHeadersAreNull() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHeaders(null);

        // Act
        java.lang.reflect.Method method = BaseRequestHandler.class.getDeclaredMethod(
                "getLocaleFromRequest", APIGatewayProxyRequestEvent.class);
        method.setAccessible(true);
        Locale result = (Locale) method.invoke(handler, requestEvent);

        // Assert
        assertNotNull(result);
        assertEquals(Locale.getDefault(), result);
    }

    @Test
    void getLocaleFromRequest_ShouldReturnDefaultLocale_WhenRequestEventIsNull() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent requestEvent = null;

        // Act
        java.lang.reflect.Method method = BaseRequestHandler.class.getDeclaredMethod(
                "getLocaleFromRequest", APIGatewayProxyRequestEvent.class);
        method.setAccessible(true);
        Locale result = (Locale) method.invoke(handler, requestEvent);

        // Assert
        assertNotNull(result);
        assertEquals(Locale.getDefault(), result);
    }

    @Test
    void getLocaleFromRequest_ShouldHandleDifferentLanguageTags() throws Exception {
        // Arrange
        Map<String, String> testCases = new HashMap<>();
        testCases.put("en", "en");
        testCases.put("en-US", "en");
        testCases.put("zh-CN", "zh");
        testCases.put("hi-IN", "hi");
        testCases.put("es", "es");

        for (Map.Entry<String, String> testCase : testCases.entrySet()) {
            APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                    .withHeaders(Collections.singletonMap("Accept-Language", testCase.getKey()));

            // Act
            java.lang.reflect.Method method = BaseRequestHandler.class.getDeclaredMethod(
                    "getLocaleFromRequest", APIGatewayProxyRequestEvent.class);
            method.setAccessible(true);
            Locale result = (Locale) method.invoke(handler, requestEvent);

            // Assert
            assertNotNull(result);
            assertEquals(testCase.getValue(), result.getLanguage(),
                    "Failed for language tag: " + testCase.getKey());
        }
    }

    @Test
    void createResponse_ShouldReturnValidResponse_WhenBodyIsValid() throws Exception {
        // Arrange
        Map<String, String> testBody = Map.of("message", "test", "status", "success");
        int statusCode = HttpStatus.OK.value();

        // Act - Use reflection to access protected method
        java.lang.reflect.Method method = BaseRequestHandler.class.getDeclaredMethod(
                "createResponse", int.class, Object.class);
        method.setAccessible(true);
        APIGatewayProxyResponseEvent result = (APIGatewayProxyResponseEvent) method.invoke(handler, statusCode, testBody);

        // Assert
        assertNotNull(result);
        assertEquals(statusCode, result.getStatusCode());
        assertNotNull(result.getBody());

        // Verify body can be parsed as JSON
        Map<String, String> parsedBody = objectMapper.readValue(result.getBody(), new TypeReference<Map<String, String>>() {});
        assertEquals("test", parsedBody.get("message"));
        assertEquals("success", parsedBody.get("status"));
    }

    @Test
    void createResponse_ShouldReturnErrorResponse_WhenSerializationFails() throws Exception {
        // Arrange
        // Create an object that will fail serialization
        Object failingObject = new Object() {
            @SuppressWarnings("unused")
            public Object getCircular() {
                return this; // This will cause issues, but we'll mock the ObjectMapper to throw
            }
        };

        // Mock ObjectMapper to throw exception
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failed"));

        // Inject mock mapper
        Field objectMapperField = BaseRequestHandler.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        objectMapperField.set(null, mockMapper);

        when(messageSource.getMessage(eq("error.internalServer"), any(Object[].class), any(Locale.class)))
                .thenReturn("Internal server error occurred");

        // Act
        java.lang.reflect.Method method = BaseRequestHandler.class.getDeclaredMethod(
                "createResponse", int.class, Object.class);
        method.setAccessible(true);
        APIGatewayProxyResponseEvent result = (APIGatewayProxyResponseEvent) method.invoke(handler, HttpStatus.OK.value(), failingObject);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatusCode());
        assertNotNull(result.getBody());

        // Verify it's an error response
        SaayamResponse<Void> errorResponse = objectMapper.readValue(
                result.getBody(),
                new TypeReference<SaayamResponse<Void>>() {}
        );
        assertFalse(errorResponse.success());
        assertEquals(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), errorResponse.saayamCode());

        // Restore original mapper
        objectMapperField.set(null, objectMapper);
    }

    @Test
    void createErrorResponse_ShouldReturnErrorResponse_WithCorrectStatusCode() throws Exception {
        // Arrange
        int statusCode = HttpStatus.BAD_REQUEST.value();
        SaayamStatusCode saayamCode = SaayamStatusCode.BAD_REQUEST;
        String message = "Invalid request";

        // Act - Use reflection to access protected method
        java.lang.reflect.Method method = BaseRequestHandler.class.getDeclaredMethod(
                "createErrorResponse", int.class, SaayamStatusCode.class, String.class);
        method.setAccessible(true);
        APIGatewayProxyResponseEvent result = (APIGatewayProxyResponseEvent) method.invoke(
                handler, statusCode, saayamCode, message);

        // Assert
        assertNotNull(result);
        assertEquals(statusCode, result.getStatusCode());
        assertNotNull(result.getBody());

        SaayamResponse<Void> errorResponse = objectMapper.readValue(
                result.getBody(),
                new TypeReference<SaayamResponse<Void>>() {}
        );
        assertFalse(errorResponse.success());
        assertEquals(saayamCode.getCode(), errorResponse.saayamCode());
        assertEquals(message, errorResponse.message());
        assertEquals(statusCode, errorResponse.statusCode());
    }

    @Test
    void createErrorResponse_ShouldHandleDifferentErrorCodes() throws Exception {
        // Arrange
        Map<SaayamStatusCode, Integer> testCases = new HashMap<>();
        testCases.put(SaayamStatusCode.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND.value());
        testCases.put(SaayamStatusCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.value());
        testCases.put(SaayamStatusCode.FORBIDDEN, HttpStatus.FORBIDDEN.value());
        testCases.put(SaayamStatusCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());

        for (Map.Entry<SaayamStatusCode, Integer> testCase : testCases.entrySet()) {
            // Act
            java.lang.reflect.Method method = BaseRequestHandler.class.getDeclaredMethod(
                    "createErrorResponse", int.class, SaayamStatusCode.class, String.class);
            method.setAccessible(true);
            APIGatewayProxyResponseEvent result = (APIGatewayProxyResponseEvent) method.invoke(
                    handler, testCase.getValue(), testCase.getKey(), "Test error");

            // Assert
            assertNotNull(result);
            assertEquals(testCase.getValue().intValue(), result.getStatusCode());

            SaayamResponse<Void> errorResponse = objectMapper.readValue(
                    result.getBody(),
                    new TypeReference<SaayamResponse<Void>>() {}
            );
            assertFalse(errorResponse.success());
            assertEquals(testCase.getKey().getCode(), errorResponse.saayamCode());
        }
    }

    @Test
    void createResponse_ShouldHandleComplexObjects() throws Exception {
        // Arrange
        SaayamResponse<Map<String, String>> complexBody = SaayamResponse.<Map<String, String>>builder()
                .success(true)
                .statusCode(200)
                .saayamCode(SaayamStatusCode.SUCCESS.getCode())
                .message("Success")
                .data(Map.of("key1", "value1", "key2", "value2"))
                .build();

        // Act
        java.lang.reflect.Method method = BaseRequestHandler.class.getDeclaredMethod(
                "createResponse", int.class, Object.class);
        method.setAccessible(true);
        APIGatewayProxyResponseEvent result = (APIGatewayProxyResponseEvent) method.invoke(
                handler, HttpStatus.OK.value(), complexBody);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertNotNull(result.getBody());

        SaayamResponse<Map<String, String>> parsed = objectMapper.readValue(
                result.getBody(),
                new TypeReference<SaayamResponse<Map<String, String>>>() {}
        );
        assertTrue(parsed.success());
        assertNotNull(parsed.data());
        assertEquals("value1", parsed.data().get("key1"));
    }

    @Test
    void createResponse_ShouldHandleNullBody() throws Exception {
        // Arrange
        int statusCode = HttpStatus.NO_CONTENT.value();

        // Act
        java.lang.reflect.Method method = BaseRequestHandler.class.getDeclaredMethod(
                "createResponse", int.class, Object.class);
        method.setAccessible(true);
        APIGatewayProxyResponseEvent result = (APIGatewayProxyResponseEvent) method.invoke(
                handler, statusCode, null);

        // Assert
        assertNotNull(result);
        assertEquals(statusCode, result.getStatusCode());
        // Body should contain "null" as JSON
        assertEquals("null", result.getBody());
    }
}

