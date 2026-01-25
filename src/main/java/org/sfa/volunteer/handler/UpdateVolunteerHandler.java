package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import lombok.extern.slf4j.Slf4j;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.exception.EnumUnspecifiedException;
import org.sfa.volunteer.exception.InvalidRequestException;
import org.sfa.volunteer.exception.LambdaExceptionHandler;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.HttpStatus;
import java.util.Map;

@Slf4j
public class UpdateVolunteerHandler extends BaseRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final VolunteerService volunteerService = context.getBean(VolunteerService.class);
    private static final ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context lambdaContext) {
        try {
            log.info("Received create request event: {}", requestEvent);


            Map<String, Object> body = parseBody(requestEvent.getBody());
            VolunteerRequest updateRequest = parseRequest(body);

            VolunteerResponse updatedVolunteer = volunteerService.updateVolunteer(updateRequest);

            SaayamResponse<VolunteerResponse> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.VOLUNTEER_UPDATED,
                    new Object[]{updatedVolunteer.userId()},
                    updatedVolunteer
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

    private VolunteerRequest parseRequest(Map<String, Object> body) {
        return objectMapper.convertValue(body, VolunteerRequest.class);
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
