package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

public class PresignProfileImageUploadHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ApplicationContext ctx = new AnnotationConfigApplicationContext(VolunteerApplication.class);
    private static final ProfileImageStorageService profileService = ctx.getBean(ProfileImageStorageService.class);
    private static final ResponseBuilder responseBuilder = ctx.getBean(ResponseBuilder.class);
    private static final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent res = new APIGatewayProxyResponseEvent();
        try {
            String userId = event.getPathParameters() == null ? null : event.getPathParameters().get("userId");
            String regionHint = header(event, "X-Dev-Region", "us-east-1");

            String contentType = q(event, "contentType");
            String contentLengthStr = q(event, "contentLength");
            if (contentType == null || contentType.isBlank()) {
                return badRequest(res, "Missing query param: contentType");
            }
            long contentLength = 0;
            if (contentLengthStr != null && !contentLengthStr.isBlank()) {
                contentLength = Long.parseLong(contentLengthStr);
            }

            Map<String, Object> payload = profileService.presignUpload(userId, contentType, contentLength, regionHint);

            SaayamResponse<Map<String, Object>> body = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    payload
            );

            res.setStatusCode(200);
            res.setHeaders(Map.of("Content-Type", "application/json"));
            res.setBody(om.writeValueAsString(body));
            return res;

        } catch (Exception e) {
            res.setStatusCode(500);
            res.setHeaders(Map.of("Content-Type", "application/json"));
            res.setBody("{\"message\":\"Internal Server Error\"}");
            return res;
        }
    }

    private static APIGatewayProxyResponseEvent badRequest(APIGatewayProxyResponseEvent res, String msg) {
        res.setStatusCode(400);
        res.setHeaders(Map.of("Content-Type", "application/json"));
        res.setBody("{\"message\":\"" + msg.replace("\"", "") + "\"}");
        return res;
    }

    private static String q(APIGatewayProxyRequestEvent e, String key) {
        if (e.getQueryStringParameters() == null) return null;
        return e.getQueryStringParameters().get(key);
    }

    private static String header(APIGatewayProxyRequestEvent e, String key, String def) {
        if (e.getHeaders() == null) return def;
        String v = e.getHeaders().get(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}