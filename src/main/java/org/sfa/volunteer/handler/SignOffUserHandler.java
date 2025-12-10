package org.sfa.volunteer.handler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.SignOffRequest;
import org.sfa.volunteer.dto.response.SignOffResponse;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class SignOffUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final UserService userService;
    private static final ProfileImageStorageService profileImageStorageService;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;

    static {
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        userService = context.getBean(UserService.class);
        profileImageStorageService = context.getBean(ProfileImageStorageService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            String lang = Optional.ofNullable(requestEvent.getHeaders())
                    .map(headers -> headers.getOrDefault("Accept-Language", "en"))
                    .orElse("en");
            Locale locale = Locale.forLanguageTag(lang);

            String userId = Optional.ofNullable(requestEvent.getPathParameters())
                    .map(params -> params.get("userId"))
                    .orElseThrow(() -> new IllegalArgumentException("userId path parameter is missing"));

            // Parse optional body for reason
            SignOffRequest signOffRequest = null;
            if (requestEvent.getBody() != null && !requestEvent.getBody().isEmpty()) {
                Map<String, Object> body = parseBody(requestEvent.getBody());
                signOffRequest = parseRequest(body);
            }

            // Delete profile image from S3
            profileImageStorageService.delete(userId, "us-east-1");

            // Sign off user
            String reason = (signOffRequest != null ? signOffRequest.reason() : null);
            SignOffResponse signOffResponse = userService.signOffUser(userId, reason);

            SaayamResponse<SignOffResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.USER_DELETED,
                    new Object[]{userId},
                    signOffResponse
            );

            String responseBody = objectMapper.writeValueAsString(successResponse);
            response.setBody(responseBody);
            response.setStatusCode(200);

        } catch (Exception e) {
            context.getLogger().log("Error processing sign-off request: " + e.getMessage());
            e.printStackTrace();

            String lang = Optional.ofNullable(requestEvent.getHeaders())
                    .map(headers -> headers.getOrDefault("Accept-Language", "en"))
                    .orElse("en");
            Locale locale = Locale.forLanguageTag(lang);
            String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null);

            SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(
                    500,
                    SaayamStatusCode.INTERNAL_SERVER_ERROR,
                    errorMessage
            );

            try {
                String responseBody = objectMapper.writeValueAsString(errorResponse);
                response.setBody(responseBody);
            } catch (Exception jsonException) {
                response.setBody("{\"message\":\"Failed to serialize error response\"}");
            }

            response.setStatusCode(500);
        }

        return response;
    }

    private SignOffRequest parseRequest(Map<String, Object> body) {
        return objectMapper.convertValue(body, SignOffRequest.class);
    }

    private Map<String, Object> parseBody(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse request body", e);
        }
    }
}