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
import org.sfa.volunteer.dto.response.NotificationPaginationResponse;
import org.sfa.volunteer.dto.response.NotificationsResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


public class GetNotificationsListHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>  {
    private static final VolunteerService volunteerService;
    private static final ResponseBuilder responseBuilder;
    private static final MessageSourceUtil messageSourceUtil;
    private static final ObjectMapper objectMapper;

    static{
        ApplicationContext context = SpringApplication.run(VolunteerApplication.class);
        volunteerService = context.getBean(VolunteerService.class);
        responseBuilder = context.getBean(ResponseBuilder.class);
        messageSourceUtil = context.getBean(MessageSourceUtil.class);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try{
            String lang = requestEvent.getHeaders().getOrDefault("Accept-Language", "en");
            Locale locale = Locale.forLanguageTag(lang);

            Map<String, String> queryStringParameters = requestEvent.getQueryStringParameters();
            String userId = Optional.ofNullable(requestEvent.getPathParameters())
                    .map(p -> p.get("userId"))
                    .orElseThrow(() -> new RuntimeException("Missing path parameter 'userId'"));
            Integer page = null;
            Integer size = null;
            LocalDateTime clientRefTime = null;
            if (queryStringParameters != null) {
                if (queryStringParameters.containsKey("page")) {
                    page = Integer.parseInt(queryStringParameters.get("page"));
                }
                if (queryStringParameters.containsKey("size")) {
                    size = Integer.parseInt(queryStringParameters.get("size"));
                }
                if (queryStringParameters.containsKey("clientRefTime")) {
                    clientRefTime = LocalDateTime.parse(queryStringParameters.get("clientRefTime"));
                }


            }
            NotificationPaginationResponse<NotificationsResponse> paginationResponse = volunteerService.getNotificationsList(userId, page, size, clientRefTime);

            SaayamResponse<NotificationPaginationResponse<NotificationsResponse>> successResponse = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    new Object[]{},
                    paginationResponse
            );

            String responseBody = objectMapper.writeValueAsString(successResponse);
            response.setBody(responseBody);
            response.setStatusCode(200); // OK
        }
        catch (Exception e){
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

            response.setStatusCode(500); // Internal Server Error
        }

        return response;



    }



}
