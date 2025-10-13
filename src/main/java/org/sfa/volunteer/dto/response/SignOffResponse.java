package org.sfa.volunteer.dto.response;

import java.time.Instant;

public record SignOffResponse(
        String userId,
        String status,
        Instant timestamp
) {}
