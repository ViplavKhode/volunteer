package org.sfa.volunteer.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user_id", columnList = "user_id"),
        @Index(name = "idx_notifications_status", columnList = "status"),
        @Index(name = "idx_notifications_type_id", columnList = "type_id"),
        @Index(name = "idx_notifications_channel_id", columnList = "channel_id")
})
@ToString
public class Notifications {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notifications_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notifications_type"))
    private NotificationTypes type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notifications_channel"))
    private NotificationChannels channel;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @jakarta.persistence.Convert(converter = org.sfa.volunteer.converter.StatusTypeConverter.class)
    @Column(name = "status", columnDefinition = "status_type")
    private StatusType status;

    @Column(name = "created_at", updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;
}
