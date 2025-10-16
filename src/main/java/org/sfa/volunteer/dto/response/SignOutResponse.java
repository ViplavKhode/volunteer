package org.sfa.volunteer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignOutResponse {
    private String message;
    private ZonedDateTime signOutTime;
    private boolean success;
}
