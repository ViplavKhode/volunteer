package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.sfa.volunteer.exception.EnumUnspecifiedException;
import org.sfa.volunteer.exception.InvalidRequestException;
import org.sfa.volunteer.exception.LambdaExceptionHandler;
import org.sfa.volunteer.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import java.util.Locale;
import org.sfa.volunteer.util.ResponseBuilder;
import java.util.Map;
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.UserProfileResponse;

@Slf4j
public class GetAllUserHandler extends BaseRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final UserService userService = context.getBean(UserService.class);
    private static final ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context lambdaContext) {
        try {
            log.info("Received create request event: {}", requestEvent);

            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
            Locale locale = Locale.forLanguageTag(lang);

            Map<String, String> queryStringParameters = requestEvent.getQueryStringParameters();
            Integer page = null;
            Integer size = null;

            if (queryStringParameters != null) {
                if (queryStringParameters.containsKey("page")) {
                    page = Integer.parseInt(queryStringParameters.get("page"));
                }
                if (queryStringParameters.containsKey("size")) {
                    size = Integer.parseInt(queryStringParameters.get("size"));
                }
            }

            PaginationResponse<UserProfileResponse> paginationResponse = userService.findAllUsersWithPagination(page, size);

            SaayamResponse<PaginationResponse<UserProfileResponse>> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    new Object[]{},
                    paginationResponse
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

    private CreateUserRequest parseRequest(Map<String, Object> body) {
        return objectMapper.convertValue(body, CreateUserRequest.class);
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

