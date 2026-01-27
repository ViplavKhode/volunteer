package org.sfa.volunteer.dto.response;

import lombok.Builder;

@Builder
public record UserExistsResponse(
        boolean exists,
        String userId,
        String matchedOn
) {
}
