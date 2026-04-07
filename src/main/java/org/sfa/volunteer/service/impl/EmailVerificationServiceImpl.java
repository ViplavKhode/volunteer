package org.sfa.volunteer.service.impl;

import org.sfa.volunteer.entities.EmailVerification;
import org.sfa.volunteer.repository.EmailVerificationRepository;
import org.sfa.volunteer.service.EmailVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 2;
    private static final int RESEND_LIMIT = 5;
    private static final long RESEND_COOLDOWN_MS = 900000; // 15 min

    @Autowired
    private EmailVerificationRepository repository;

    @Override
    public void sendVerificationCode(String email, Long userId, boolean useMagicLink) {
        EmailVerification verification = repository.findByEmail(email).orElse(new EmailVerification());
        verification.setUserId(userId);
        verification.setEmail(email);
        verification.setVerified(false);
        verification.setResendCount(0);
        verification.setLastSentTime(new Date());
        verification.setExpiryTime(new Date(System.currentTimeMillis() + EXPIRY_MINUTES * 60 * 1000));
        if (useMagicLink) {
            verification.setToken(UUID.randomUUID().toString());
            verification.setCode(null);
        } else {
            verification.setCode(generateCode());
            verification.setToken(null);
        }
        repository.save(verification);
        // TODO: Send email logic here
    }

    @Override
    public boolean verifyCode(String email, String code) {
        Optional<EmailVerification> opt = repository.findByEmailAndCode(email, code);
        if (opt.isPresent()) {
            EmailVerification v = opt.get();
            if (!v.isVerified() && v.getExpiryTime().after(new Date())) {
                v.setVerified(true);
                repository.save(v);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean verifyToken(String token) {
        Optional<EmailVerification> opt = repository.findByToken(token);
        if (opt.isPresent()) {
            EmailVerification v = opt.get();
            if (!v.isVerified() && v.getExpiryTime().after(new Date())) {
                v.setVerified(true);
                repository.save(v);
                return true;
            }
        }
        return false;
    }

    @Override
    public void resendVerificationCode(String email, Long userId, boolean useMagicLink) {
        Optional<EmailVerification> opt = repository.findByEmail(email);
        if (opt.isPresent()) {
            EmailVerification v = opt.get();
            if (v.getResendCount() >= RESEND_LIMIT || (new Date().getTime() - v.getLastSentTime().getTime()) < RESEND_COOLDOWN_MS) {
                throw new RuntimeException("Resend limit reached or cooldown active");
            }
            v.setResendCount(v.getResendCount() + 1);
            v.setLastSentTime(new Date());
            v.setExpiryTime(new Date(System.currentTimeMillis() + EXPIRY_MINUTES * 60 * 1000));
            if (useMagicLink) {
                v.setToken(UUID.randomUUID().toString());
                v.setCode(null);
            } else {
                v.setCode(generateCode());
                v.setToken(null);
            }
            repository.save(v);
            // TODO: Send email logic here
        } else {
            sendVerificationCode(email, userId, useMagicLink);
        }
    }

    @Override
    public EmailVerification getVerificationByEmail(String email) {
        return repository.findByEmail(email).orElse(null);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
