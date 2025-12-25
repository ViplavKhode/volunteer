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

import java.util.Map;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class DeleteProfileImageHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ConfigurableApplicationContext ctx;
    static {
        String profile = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev");
        ctx = new SpringApplicationBuilder(VolunteerApplication.class).profiles(profile).web(WebApplicationType.NONE).run();
    }
    private static final ProfileImageStorageService profileService = ctx.getBean(ProfileImageStorageService.class);
    private static final ResponseBuilder responseBuilder = ctx.getBean(ResponseBuilder.class);
    private static final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent res = new APIGatewayProxyResponseEvent();
        try {
            String userId = event.getPathParameters() == null ? null : event.getPathParameters().get("userId");
            String regionHint = header(event, "X-Dev-Region", "us-east-1");

            profileService.delete(userId, regionHint);

            SaayamResponse<Map<String, Object>> out = responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS,
                    Map.of("userId", userId, "message", "Profile image deleted")
            );

            res.setStatusCode(200);
            res.setHeaders(Map.of("Content-Type", "application/json"));
            res.setBody(om.writeValueAsString(out));
            return res;

        } catch (Exception e) {
            res.setStatusCode(500);
            res.setHeaders(Map.of("Content-Type", "application/json"));
            res.setBody("{\"message\":\"Internal Server Error\"}");
            return res;
        }
    }

    private static String header(APIGatewayProxyRequestEvent e, String key, String def) {
        if (e.getHeaders() == null) return def;
        String v = e.getHeaders().get(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}