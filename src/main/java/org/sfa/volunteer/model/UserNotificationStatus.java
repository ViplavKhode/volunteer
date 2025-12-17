package org.sfa.volunteer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Table(name = "user_notification_status")
public class UserNotificationStatus {

    @Id
    @Column(name = "user_id")
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // connecting this entity's ID to the User's ID
    @JoinColumn(name = "user_id")
    @ToString.Exclude // Prevents infinite loops
    @EqualsAndHashCode.Exclude // Prevents recursion in equals/hashcode
    private User user;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

}
