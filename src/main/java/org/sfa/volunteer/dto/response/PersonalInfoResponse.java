package org.sfa.volunteer.dto.response;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record PersonalInfoResponse(
        String id,
        String firstName,
        String middleName,
        String lastName,
        String fullName,
        String primaryEmailAddress,
        String primaryPhoneNumber,
        String timeZone,
        String gender,
        ZonedDateTime lastUpdateDate
) {
}