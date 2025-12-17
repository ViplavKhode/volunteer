package org.sfa.volunteer.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@Table(name = "notification_channels")
public class NotificationChannels {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_id")
    private Long channel_id;

    @Column(name = "channel_name", nullable = false, unique = true, length = 255)
    private String channel_name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
