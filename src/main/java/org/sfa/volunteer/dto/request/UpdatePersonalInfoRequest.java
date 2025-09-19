package org.sfa.volunteer.dto.request;

import lombok.Builder;

@Builder
public record UpdatePersonalInfoRequest(
        String firstName,
        String middleName,
        String lastName,
        String fullName,
        String primaryEmailAddress,
        String primaryPhoneNumber,
        String timeZone,
        String gender
) {
}