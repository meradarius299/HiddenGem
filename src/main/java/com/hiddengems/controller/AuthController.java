package com.hiddengems.controller;

import com.hiddengems.dto.AuthResponse;
import com.hiddengems.dto.AuthTokenRequest;
import com.hiddengems.dto.EmailRequest;
import com.hiddengems.dto.GoogleLoginRequest;
import com.hiddengems.dto.LoginRequest;
import com.hiddengems.dto.MessageResponse;
import com.hiddengems.dto.PasswordResetRequest;
import com.hiddengems.dto.RegisterRequest;
import com.hiddengems.service.AccountEmailService;
import com.hiddengems.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountEmailService accountEmailService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody AuthTokenRequest request) {
        accountEmailService.verifyEmail(request.token());
        return new MessageResponse("Email verified. You can now sign in.");
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody EmailRequest request) {
        accountEmailService.resendVerification(request.email());
        return new MessageResponse("If the account needs verification, a new email will be sent shortly.");
    }

    @PostMapping("/password/forgot")
    public MessageResponse forgotPassword(@Valid @RequestBody EmailRequest request) {
        accountEmailService.requestPasswordReset(request.email());
        return new MessageResponse("If an account exists for that address, a password reset email will arrive shortly.");
    }

    @PostMapping("/password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        accountEmailService.resetPassword(request.token(), request.password());
        return new MessageResponse("Password updated. You can now sign in with your new password.");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request);
    }
}
