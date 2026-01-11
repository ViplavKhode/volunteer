package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.service.ProfileImageStorageService;

import java.util.HashMap;
import java.util.Map;

public class UploadProfileImageHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final ConfigurableApplicationContext CTX =
            new SpringApplicationBuilder(VolunteerApplication.class)
                    .web(WebApplicationType.NONE)
                    .properties(
                            "spring.main.web-application-type=none",
                            "spring.autoconfigure.exclude=org.springframework.cloud.function.serverless.web.ServerlessAutoConfiguration"
                    )
                    .run();

    private final ProfileImageStorageService storage = CTX.getBean(ProfileImageStorageService.class);

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            String region = header(event, "X-Dev-Region", "us-east-1");

            String bodyJson = asString(event.get("body"));
            Map<String, Object> body = MAPPER.readValue(bodyJson, new TypeReference<>() {});

            String userId = asString(body.get("userId"));
            String contentType = asString(body.get("contentType"));
            String base64 = asString(body.get("base64"));

            if (isBlank(userId) || isBlank(contentType) || isBlank(base64)) {
                return apiError(400, "userId, contentType, base64 are required");
            }

            Map<String, Object> result = storage.uploadBase64(userId, contentType, base64, region);

            return apiJson(200, Map.of(
                    "message", "Profile image uploaded",
                    "data", result
            ));

        } catch (Exception e) {
            return apiError(500, safeMsg(e));
        }
    }

    // ---------- helpers ----------
    private static String header(Map<String, Object> event, String name, String def) {
        Object headersObj = event.get("headers");
        if (!(headersObj instanceof Map)) return def;

        @SuppressWarnings("unchecked")
        Map<String, Object> headers = (Map<String, Object>) headersObj;

        for (Map.Entry<String, Object> en : headers.entrySet()) {
            if (en.getKey() != null && en.getKey().equalsIgnoreCase(name)) {
                String v = asString(en.getValue());
                return isBlank(v) ? def : v;
            }
        }
        return def;
    }

    private static Map<String, Object> apiJson(int status, Object payload) throws Exception {
        Map<String, Object> resp = new HashMap<>();
        resp.put("statusCode", status);
        resp.put("headers", Map.of("Content-Type", "application/json"));
        resp.put("isBase64Encoded", false);
        resp.put("body", MAPPER.writeValueAsString(payload));
        return resp;
    }

    private static Map<String, Object> apiError(int status, String msg) {
        try {
            return apiJson(status, Map.of("message", msg));
        } catch (Exception e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("statusCode", status);
            resp.put("headers", Map.of("Content-Type", "application/json"));
            resp.put("isBase64Encoded", false);
            resp.put("body", "{\"message\":\"" + msg.replace("\"", "'") + "\"}");
            return resp;
        }
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String safeMsg(Exception e) {
        String m = e.getMessage();
        return (m == null || m.isBlank()) ? e.getClass().getSimpleName() : m;
    }
}