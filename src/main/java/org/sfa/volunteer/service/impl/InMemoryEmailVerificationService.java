package org.sfa.volunteer.service.impl;

import org.sfa.volunteer.service.EmailVerificationService;
import org.sfa.volunteer.service.EmailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryEmailVerificationService implements EmailVerificationService {
    @Autowired
    private EmailSenderService emailSenderService;
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 10;
    private static final int RESEND_LIMIT = 5;
    private static final long RESEND_COOLDOWN_MS = 60000; // 1 minute

    private static class VerificationData {
        Long userId;
        String email;
        String code;
        String token;
        boolean verified;
        int resendCount;
        long lastSentTime;
        long expiryTime;
    }

    private final Map<String, VerificationData> emailMap = new ConcurrentHashMap<>();
    private final Map<String, VerificationData> tokenMap = new ConcurrentHashMap<>();
    @Override
    public void sendVerificationCode(String email, Long userId, boolean useMagicLink) {
        VerificationData verification = new VerificationData();
        verification.userId = userId;
        verification.email = email;
        verification.verified = false;
        verification.resendCount = 0;
        verification.lastSentTime = System.currentTimeMillis();
        verification.expiryTime = System.currentTimeMillis() + EXPIRY_MINUTES * 60 * 1000;
        if (useMagicLink) {
            String token = UUID.randomUUID().toString();
            verification.token = token;
            verification.code = null;
            tokenMap.put(token, verification);
            String link = "https://your-app.com/verify?token=" + token;
            emailSenderService.sendEmail(email, "Your Magic Login Link", "Click this link to verify: " + link);
        } else {
            String code = generateCode();
            verification.code = code;
            verification.token = null;
            emailSenderService.sendEmail(email, "Your Verification Code", "Your verification code is: " + code);
        }
        emailMap.put(email, verification);
    }
    @Override
    public boolean verifyCode(String email, String code) {
        VerificationData v = emailMap.get(email);
        if (v != null && !v.verified && v.code != null && v.code.equals(code) && v.expiryTime > System.currentTimeMillis()) {
            v.verified = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean verifyToken(String token) {
        VerificationData v = tokenMap.get(token);
        if (v != null && !v.verified && v.expiryTime > System.currentTimeMillis()) {
            v.verified = true;
            return true;
        }
        return false;
    }

    @Override
    public void resendVerificationCode(String email, Long userId, boolean useMagicLink) {
        VerificationData v = emailMap.get(email);
        if (v != null) {
            if (v.resendCount >= RESEND_LIMIT || (System.currentTimeMillis() - v.lastSentTime) < RESEND_COOLDOWN_MS) {
                throw new RuntimeException("Resend limit reached or cooldown active");
            }
            v.resendCount++;
            v.lastSentTime = System.currentTimeMillis();
            v.expiryTime = System.currentTimeMillis() + EXPIRY_MINUTES * 60 * 1000;
            if (useMagicLink) {
                String token = UUID.randomUUID().toString();
                v.token = token;
                v.code = null;
                tokenMap.put(token, v);
                String link = "https://your-app.com/verify?token=" + token;
                emailSenderService.sendEmail(email, "Your Magic Login Link", "Click this link to verify: " + link);
            } else {
                String code = generateCode();
                v.code = code;
                v.token = null;
                emailSenderService.sendEmail(email, "Your Verification Code", "Your verification code is: " + code);
            }
        } else {
            sendVerificationCode(email, userId, useMagicLink);
        }
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
