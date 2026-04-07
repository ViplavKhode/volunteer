package org.sfa.volunteer.repository;

import org.sfa.volunteer.entities.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmail(String email);
    Optional<EmailVerification> findByToken(String token);
    Optional<EmailVerification> findByEmailAndCode(String email, String code);
}
