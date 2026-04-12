package org.sfa.volunteer.controller;

import org.sfa.volunteer.dto.request.EmailVerificationRequest;
import org.sfa.volunteer.dto.request.VerifyCodeRequest;
import org.sfa.volunteer.dto.request.VerifyTokenRequest;
import org.sfa.volunteer.service.EmailVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/0.0.1/email-verification")
public class EmailVerificationController {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @GetMapping("/healthCheck")
    public String health(){
        return "Connection is healthy";
    }

    @PostMapping("/request")
    public ResponseEntity<?> requestVerification(@RequestBody EmailVerificationRequest request) {
        emailVerificationService.sendVerificationCode(request.getEmail(), request.getUserId(), request.isUseMagicLink());
        return ResponseEntity.ok("Verification sent");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody VerifyCodeRequest request) {
        boolean result = emailVerificationService.verifyCode(request.getEmail(), request.getCode());
        return result ? ResponseEntity.ok("Verified") : ResponseEntity.badRequest().body("Invalid or expired code");
    }

    @PostMapping("/verify-token")
    public ResponseEntity<?> verifyToken(@RequestBody VerifyTokenRequest request) {
        boolean result = emailVerificationService.verifyToken(request.getToken());
        return result ? ResponseEntity.ok("Verified") : ResponseEntity.badRequest().body("Invalid or expired token");
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerification(@RequestBody EmailVerificationRequest request) {
        emailVerificationService.resendVerificationCode(request.getEmail(), request.getUserId(), request.isUseMagicLink());
        return ResponseEntity.ok("Verification resent");
    }
}
