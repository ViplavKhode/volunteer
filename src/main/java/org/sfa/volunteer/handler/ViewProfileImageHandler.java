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

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ViewProfileImageHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

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

            if (isBlank(userId)) {
                return apiError(400, "userId is required");
            }

            var imgOpt = storage.download(userId, region); // Optional<DownloadedImage>
            if (imgOpt.isEmpty()) {
                return apiNoContent();
            }

            var img = imgOpt.get();
            String base64Body = Base64.getEncoder().encodeToString(img.bytes());

            Map<String, Object> resp = new HashMap<>();
            resp.put("statusCode", 200);
            resp.put("headers", Map.of(
                    "Content-Type", img.contentType()
            ));
            resp.put("isBase64Encoded", true);
            resp.put("body", base64Body);
            return resp;

        } catch (Exception e) {
            return apiError(500, safeMsg(e));
        }
    }

    // ---------- helpers ----------
    private static Map<String, Object> apiNoContent() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("statusCode", 204);
        resp.put("headers", Map.of());
        resp.put("isBase64Encoded", false);
        resp.put("body", "");
        return resp;
    }

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

    private static Map<String, Object> apiError(int status, String msg) {
        try {
            Map<String, Object> resp = new HashMap<>();
            resp.put("statusCode", status);
            resp.put("headers", Map.of("Content-Type", "application/json"));
            resp.put("isBase64Encoded", false);
            resp.put("body", MAPPER.writeValueAsString(Map.of("message", msg)));
            return resp;
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