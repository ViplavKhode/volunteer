package org.sfa.volunteer.dto.request;

import lombok.Builder;

@Builder
public record SignOffRequest( String reason) {
}
