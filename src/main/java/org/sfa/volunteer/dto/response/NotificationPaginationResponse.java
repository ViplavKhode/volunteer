package org.sfa.volunteer.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPaginationResponse<T> {
    private List<T> items;
    private int currentPage;
    private long totalItems;
    private int totalPages;
    private LocalDateTime referenceTimestamp;
}

