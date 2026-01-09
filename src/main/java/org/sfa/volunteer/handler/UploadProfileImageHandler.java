package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UploadProfileImageHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
            String userId = readUserId(event);
            String region = readHeader(event, "X-Dev-Region", "us-east-1");

            Map<String, Object> body = readBodyAsMap(event);

            String base64 = asString(body.get("base64"));
            String contentType = asString(body.getOrDefault("contentType", "image/jpeg"));

            if (userId == null || userId.isBlank()) {
                return error(400, "userId is required");
            }
            if (base64 == null || base64.isBlank()) {
                return error(400, "base64 is required");
            }

            Map<String, Object> result = storage.uploadBase64(userId, base64, contentType, region);

            return ok(result);

        } catch (Exception e) {
            return error(500, e.getMessage());
        }
    }

    // -------- helpers --------

    private static Map<String, Object> readBodyAsMap(Map<String, Object> event) throws Exception {
        Object bodyObj = event.get("body");
        if (bodyObj == null) return Collections.emptyMap();

        if (bodyObj instanceof Map<?, ?> m) {
            Map<String, Object> out = new HashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        String bodyStr = String.valueOf(bodyObj);
        if (bodyStr.isBlank()) return Collections.emptyMap();

        return MAPPER.readValue(bodyStr, new TypeReference<Map<String, Object>>() {});
    }

    private static String readUserId(Map<String, Object> event) throws Exception {
        Object pp = event.get("pathParameters");
        if (pp instanceof Map<?, ?> m) {
            Object v = m.get("userId");
            if (v != null) return String.valueOf(v);
        }

        Map<String, Object> body = readBodyAsMap(event);
        Object v = body.get("userId");
        return v == null ? null : String.valueOf(v);
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

    private static String asString(Object o) {
        return (o == null) ? null : String.valueOf(o);
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