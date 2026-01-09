package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

public class DeleteProfileImageHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final ConfigurableApplicationContext ctx =
            new SpringApplicationBuilder(VolunteerApplication.class)
                    .web(WebApplicationType.NONE)
                    .properties(
                            "spring.main.web-application-type=none",
                            "spring.autoconfigure.exclude=org.springframework.cloud.function.serverless.web.ServerlessAutoConfiguration"
                    )
                    .run();

    private final ProfileImageStorageService storage =
            ctx.getBean(ProfileImageStorageService.class);

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            String userId = readPathUserId(event);
            String region = readHeader(event, "X-Dev-Region", "us-east-1");

            if (userId == null || userId.isBlank()) {
                return error(400, "userId is required in pathParameters");
            }

            storage.delete(userId, region);

            return ok(Map.of("message", "Profile image deleted", "userId", userId));

        } catch (Exception e) {
            return error(500, e.getMessage());
        }
    }

    private static String readPathUserId(Map<String, Object> event) {
        Object pp = event.get("pathParameters");
        if (pp instanceof Map<?, ?> m) {
            Object v = m.get("userId");
            if (v != null) return String.valueOf(v);
        }
        return null;
    }

    private static String readHeader(Map<String, Object> event, String headerName, String defaultVal) {
        Object headers = event.get("headers");
        if (headers instanceof Map<?, ?> m) {
            Object v = m.get(headerName);
            if (v == null) v = m.get(headerName.toLowerCase());
            if (v != null && !String.valueOf(v).isBlank()) return String.valueOf(v);
        }
        return defaultVal;
    }

    private static Map<String, Object> ok(Object body) {
        return Map.of(
                "statusCode", 200,
                "headers", Map.of("Content-Type", "application/json"),
                "body", body
        );
    }

    private static Map<String, Object> error(int status, String msg) {
        return Map.of(
                "statusCode", status,
                "headers", Map.of("Content-Type", "application/json"),
                "body", Map.of("message", msg)
        );
    }
}