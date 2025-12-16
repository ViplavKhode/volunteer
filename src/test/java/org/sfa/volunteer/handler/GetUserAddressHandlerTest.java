package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.AddressStatusResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetUserAddressHandlerTest {
    @Mock
    private UserService userService;

    @Mock
    private ResponseBuilder responseBuilder;

    @Mock
    private MessageSourceUtil messageSourceUtil;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    @InjectMocks
    private GetUserAddressStatusHandler handler;



    @Test
    void testHandleRequest_success() throws Exception {
        String userId = "ID_121";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPathParameters(Map.of("userId", userId));

        AddressStatusResponse mockResponse = new AddressStatusResponse(userId, true);
        SaayamResponse<AddressStatusResponse> mockSaayamResponse = SaayamResponse.success(SaayamStatusCode.SUCCESS, "", mockResponse);

        when(userService.getAddressStatus(userId)).thenReturn(mockResponse);
        when(responseBuilder.buildSuccessResponse(eq(SaayamStatusCode.SUCCESS), any(), eq(mockResponse))).thenReturn(mockSaayamResponse);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
        SaayamResponse<AddressStatusResponse> responseData = objectMapper.readValue(response.getBody(), new TypeReference<>() {});

        assertEquals(200, response.getStatusCode());
        assertTrue(responseData.data().addressAvailable());
        assertEquals(userId, responseData.data().userId());
    }

    @Test
    void testHandleRequest_noUserFound() throws Exception {
        String userId = "ID_121";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPathParameters(Map.of("userId", userId));

        when(userService.getAddressStatus(userId)).thenThrow(new UserNotFoundException(userId));

        String errorMessage = SaayamStatusCode.INTERNAL_SERVER_ERROR.toString();
        SaayamResponse<Void> mockSaayamResponse = SaayamResponse.error(500, SaayamStatusCode.INTERNAL_SERVER_ERROR, errorMessage);

        when(messageSourceUtil.getMessage(eq(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode()), any())).thenReturn(errorMessage);
        when(responseBuilder.<Void>buildErrorResponse(eq(500), eq(SaayamStatusCode.INTERNAL_SERVER_ERROR), eq(errorMessage))).thenReturn(mockSaayamResponse);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
        SaayamResponse<Void> responseData = objectMapper.readValue(response.getBody(), new TypeReference<>() {});

        assertEquals(500, response.getStatusCode());
        assertEquals(errorMessage, responseData.message());
    }

    @Test
    void testHandleRequest_missingUserId() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();

        String errorMessage = SaayamStatusCode.INTERNAL_SERVER_ERROR.toString();
        SaayamResponse<Void> mockSaayamResponse = SaayamResponse.error(500, SaayamStatusCode.INTERNAL_SERVER_ERROR, errorMessage);

        when(messageSourceUtil.getMessage(eq(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode()), any())).thenReturn(errorMessage);
        when(responseBuilder.<Void>buildErrorResponse(eq(500), eq(SaayamStatusCode.INTERNAL_SERVER_ERROR), eq(errorMessage))).thenReturn(mockSaayamResponse);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
        SaayamResponse<Void> responseData = objectMapper.readValue(response.getBody(), new TypeReference<>() {});

        assertEquals(500, response.getStatusCode());
        assertEquals(errorMessage, responseData.message());
    }
}
