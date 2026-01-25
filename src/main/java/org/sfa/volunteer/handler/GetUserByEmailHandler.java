package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import lombok.extern.slf4j.Slf4j;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.FindUserProfileUsingEmail;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.exception.EnumUnspecifiedException;
import org.sfa.volunteer.exception.InvalidRequestException;
import org.sfa.volunteer.exception.LambdaExceptionHandler;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class GetUserByEmailHandler  extends BaseRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final UserService userService = context.getBean(UserService.class);
    private static final ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context lambdaContext) {
        try {
            log.info("Received create request event: {}", requestEvent);

            Map<String, Object> body = parseBody(requestEvent.getBody());
            FindUserProfileUsingEmail request = parseRequest(body);
            String email = request.email();

            UserProfileResponse profileByEmail = userService.getUserProfileByEmail(email);

            SaayamResponse<UserProfileResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    new Object[]{profileByEmail.countryName()},
                    profileByEmail
            );
            log.info("Create request successful. Response: {}", successResponse);
            return createResponse(HttpStatus.CREATED.value(), successResponse);
        } catch (EnumUnspecifiedException e) {
            log.warn("EnumUnspecifiedException in CreateRequestHandler: ", e);
            return createErrorResponse(HttpStatus.BAD_REQUEST.value(), SaayamStatusCode.ENUM_UNSPECIFIED, e.getMessage());
        } catch (InvalidRequestException e) {
            log.warn("InvalidRequestException in CreateRequestHandler: ", e);
            return createErrorResponse(HttpStatus.BAD_REQUEST.value(), SaayamStatusCode.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in CreateRequestHandler: ", e);
            return LambdaExceptionHandler.handleException(e, lambdaContext, getLocaleFromRequest(requestEvent));
        }
    }

    private FindUserProfileUsingEmail parseRequest(Map<String, Object> body) {
        return objectMapper.convertValue(body, FindUserProfileUsingEmail.class);
    }

    private Map<String, Object> parseBody(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            // todo: define a customized error
            throw new RuntimeException("Failed to parse request body", e);
        }
    }
}


//            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
//            Locale locale = Locale.forLanguageTag(lang);
//            String lang = Optional.ofNullable(requestEvent.getHeaders())
//                    .map(headers -> headers.getOrDefault("Accept-Language", "en"))
//                    .orElse("en");
//            Locale locale = Locale.forLanguageTag(lang);






