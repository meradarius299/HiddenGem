package com.hiddengems.service;

import com.hiddengems.entity.AuthActionPurpose;
import com.hiddengems.entity.AuthActionToken;
import com.hiddengems.entity.AuthProvider;
import com.hiddengems.entity.User;
import com.hiddengems.repository.AuthActionTokenRepository;
import com.hiddengems.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class AccountEmailService {

    private static final Logger log = LoggerFactory.getLogger(AccountEmailService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthActionTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;
    @Value("${app.mail.from:no-reply@hiddengems.local}")
    private String from;
    @Value("${app.auth.verification-token-minutes:1440}")
    private long verificationMinutes;
    @Value("${app.auth.reset-token-minutes:30}")
    private long resetMinutes;
    @Value("${app.auth.token-resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    public void sendVerification(User user) {
        issueAndSend(user, AuthActionPurpose.VERIFY_EMAIL);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        AuthActionToken token = requireValidToken(rawToken, AuthActionPurpose.VERIFY_EMAIL);
        token.getUser().setEmailVerified(true);
        token.setUsedAt(LocalDateTime.now());
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmailIgnoreCase(email.trim())
                .filter(user -> user.getProvider() == AuthProvider.LOCAL && !user.isEmailVerified())
                .ifPresent(user -> sendBestEffort(user, AuthActionPurpose.VERIFY_EMAIL));
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(email.trim())
                .filter(user -> user.getProvider() == AuthProvider.LOCAL && user.isEmailVerified())
                .ifPresent(user -> sendBestEffort(user, AuthActionPurpose.RESET_PASSWORD));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        AuthActionToken token = requireValidToken(rawToken, AuthActionPurpose.RESET_PASSWORD);
        User user = token.getUser();
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new ResponseStatusException(BAD_REQUEST, "This account uses Google sign-in.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        token.setUsedAt(LocalDateTime.now());
    }

    private void sendBestEffort(User user, AuthActionPurpose purpose) {
        try {
            issueAndSend(user, purpose);
        } catch (MailException exception) {
            // Keep forgot/resend responses identical for existing and unknown addresses.
            tokenRepository.deleteByUserIdAndPurposeAndUsedAtIsNull(user.getId(), purpose);
            log.warn("Could not send {} email for user id {}", purpose, user.getId(), exception);
        }
    }

    private void issueAndSend(User user, AuthActionPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        var latest = tokenRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose);
        if (latest.isPresent() && latest.get().getCreatedAt().isAfter(now.minusSeconds(resendCooldownSeconds))) {
            return;
        }

        tokenRepository.deleteByUserIdAndPurposeAndUsedAtIsNull(user.getId(), purpose);
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);

        AuthActionToken token = new AuthActionToken();
        token.setUser(user);
        token.setPurpose(purpose);
        token.setTokenHash(hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusMinutes(purpose == AuthActionPurpose.VERIFY_EMAIL
                ? verificationMinutes : resetMinutes));
        tokenRepository.save(token);

        String actionUrl = publicUrl.replaceAll("/+$", "") + (purpose == AuthActionPurpose.VERIFY_EMAIL
                ? "/verify-email?token=" : "/reset-password?token=") + rawToken;
        sendEmail(user.getEmail(), purpose, actionUrl);
    }

    private AuthActionToken requireValidToken(String rawToken, AuthActionPurpose purpose) {
        AuthActionToken token = tokenRepository.findByTokenHashAndPurposeAndUsedAtIsNull(hash(rawToken), purpose)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "This link is invalid or has expired."));
        if (!token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "This link is invalid or has expired.");
        }
        return token;
    }

    private void sendEmail(String recipient, AuthActionPurpose purpose, String actionUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "Hidden Gems");
            helper.setTo(recipient);
            if (purpose == AuthActionPurpose.VERIFY_EMAIL) {
                helper.setSubject("Verify your Hidden Gems email");
                helper.setText("Welcome to Hidden Gems!\n\nOpen this link to verify your email address:\n"
                        + actionUrl + "\n\nThis link expires in " + verificationMinutes / 60 + " hours.\n"
                        + "If you did not create this account, you can ignore this email.");
            } else {
                helper.setSubject("Reset your Hidden Gems password");
                helper.setText("We received a request to reset your password.\n\nOpen this link to choose a new password:\n"
                        + actionUrl + "\n\nThis link expires in " + resetMinutes + " minutes.\n"
                        + "If you did not request this, you can ignore this email.");
            }
            mailSender.send(message);
        } catch (Exception exception) {
            if (exception instanceof MailException mailException) throw mailException;
            throw new org.springframework.mail.MailSendException("Could not create the account email", exception);
        }
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
