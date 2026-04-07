package org.sfa.volunteer.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "email_verification")
public class EmailVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email")
    private String email;

    @Column(name = "code")
    private String code;

    @Column(name = "token")
    private String token;

    @Column(name = "expiry_time")
    private Date expiryTime;

    @Column(name = "verified")
    private boolean verified;

    @Column(name = "resend_count")
    private int resendCount;

    @Column(name = "last_sent_time")
    private Date lastSentTime;
}
