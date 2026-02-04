package org.sfa.volunteer.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsResponse {

    private String type;
    private String title;
    private String message;
    private LocalDateTime date;
    private Boolean status;
}
