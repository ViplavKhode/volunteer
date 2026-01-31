package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.service.UserService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.sfa.volunteer.util.Cors;

public class DeleteProfileImageHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

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
    private final UserService userService = CTX.getBean(UserService.class);

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            String region = header(event, "X-Dev-Region", "us-east-1");

            Map<String, Object> body = readJsonBody(event);
            String userId = asString(body.get("userId"));
            if (isBlank(userId)) return apiError(400, "userId is required");

            // AUTH
            AuthInfo auth = authInfoFromEvent(event);
            authorizeUser(auth, userId);

            storage.delete(userId, region);

            Map<String, Object> resp = apiJson(200, Map.of(
                    "message", "Profile image deleted",
                    "userId", userId
            ));
            debugResponse(context, "DELETE_OK", resp);
            return resp;

        } catch (Forbidden ex) {
            Map<String, Object> resp = apiError(403, ex.getMessage());
            debugResponse(context, "DELETE_403", resp);
            return resp;
        } catch (Exception e) {
            Map<String, Object> resp = apiError(500, safeMsg(e));
            debugResponse(context, "DELETE_500", resp);
            return resp;
        }
    }

    // ---------------- AUTH ----------------

    private static class Forbidden extends RuntimeException {
        Forbidden(String msg) { super(msg); }
    }

    private record AuthInfo(String email, String groups, boolean present) {}

    private AuthInfo authInfoFromEvent(Map<String, Object> event) {
        Map<String, Object> claims = claims(event);

        String email = asString(claims.get("email"));
        if (isBlank(email)) email = asString(claims.get("cognito:username"));
        if (isBlank(email)) email = asString(claims.get("username"));

        String groups = null;
        Object g = claims.get("cognito:groups");
        if (g instanceof String s) groups = s;
        else if (g instanceof Iterable<?> it) {
            StringBuilder sb = new StringBuilder();
            for (Object o : it) {
                if (sb.length() > 0) sb.append(",");
                sb.append(String.valueOf(o));
            }
            groups = sb.toString();
        }

        boolean present = !claims.isEmpty();
        return new AuthInfo(email, groups, present);
    }

    private void authorizeUser(AuthInfo auth, String targetUserId) {
        if (!auth.present) throw new Forbidden("Missing Cognito authorizer/JWT claims");

        boolean isAdmin = auth.groups != null && auth.groups.toLowerCase().contains("admin");
        if (isAdmin) return;

        if (isBlank(auth.email)) throw new Forbidden("JWT email missing");

        UserProfileResponse caller;
        try {
            caller = userService.getUserProfileByEmail(auth.email);
        } catch (Exception e) {
            throw new Forbidden("JWT user not mapped to DB user");
        }

        String callerSid = caller.id();
        if (isBlank(callerSid)) throw new Forbidden("JWT user not mapped to DB user");
        if (!callerSid.equals(targetUserId)) throw new Forbidden("Not allowed");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> claims(Map<String, Object> event) {
        Object rc = event.get("requestContext");
        if (!(rc instanceof Map)) return Map.of();

        Object auth = ((Map<String, Object>) rc).get("authorizer");
        if (!(auth instanceof Map)) return Map.of();

        Object c = ((Map<String, Object>) auth).get("claims");
        if (c instanceof Map<?, ?> m) return (Map<String, Object>) m;

        return Map.of();
    }

    // ---------------- helpers ----------------

    private static Map<String, Object> readJsonBody(Map<String, Object> event) throws Exception {
        String body = asString(event.get("body"));
        if (body == null) body = "";

        boolean isB64 = Boolean.TRUE.equals(event.get("isBase64Encoded"));
        if (isB64 && !body.isBlank()) {
            body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
        }

        if (body.isBlank()) return Map.of();
        return MAPPER.readValue(body, new TypeReference<>() {});
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

    private static Map<String, Object> apiJson(int status, Object payload) throws Exception {
        Map<String, Object> resp = new HashMap<>();
        resp.put("statusCode", status);
        resp.put("headers", Cors.headers("DELETE,OPTIONS"));
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
            resp.put("headers", Cors.headers("DELETE,OPTIONS"));
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
    @SuppressWarnings("unchecked")
    private static void debugResponse(Context context, String label, Map<String, Object> resp) {
        if (context == null) return;
        try {
            Object h = resp.get("headers");
            context.getLogger().log(label + " statusCode=" + resp.get("statusCode") + "\n");
            context.getLogger().log(label + " headers=" + String.valueOf(h) + "\n");
            context.getLogger().log(label + " isBase64Encoded=" + resp.get("isBase64Encoded") + "\n");
        } catch (Exception e) {
            context.getLogger().log(label + " debug failed: " + e.getMessage() + "\n");
        }
    }
}