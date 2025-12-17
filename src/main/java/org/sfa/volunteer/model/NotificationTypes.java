package org.sfa.volunteer.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notification_types")
public class NotificationTypes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Long typeId;

    @Column(name = "type_name", nullable = false, unique = true, length = 255)
    private String type_name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

}
