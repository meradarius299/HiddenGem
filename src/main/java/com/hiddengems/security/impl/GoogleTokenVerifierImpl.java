package com.hiddengems.security.impl;

import com.hiddengems.security.GoogleTokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valideaza tokenul Google: semnatura (cheile publice Google), expirarea, emitentul si audienta
 * (trebuie sa fie unul dintre client ID-urile din app.google.client-ids).
 */
@Component
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final Set<String> allowedAudiences;
    private final JwtDecoder decoder;

    public GoogleTokenVerifierImpl(@Value("${app.google.client-ids:}") String clientIds) {
        this.allowedAudiences = Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        OAuth2TokenValidator<Jwt> issuerValidator = jwt ->
                GOOGLE_ISSUERS.contains(jwt.getClaimAsString("iss"))
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Emitent invalid", null));

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> audience = jwt.getAudience();
            boolean ok = audience != null && audience.stream().anyMatch(allowedAudiences::contains);
            return ok
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_token", "Audienta invalida", null));
        };

        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(), issuerValidator, audienceValidator));
        this.decoder = nimbus;
    }

    @Override
    public GoogleUser verify(String idToken) {
        if (allowedAudiences.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Login cu Google nu este configurat (app.google.client-ids)");
        }
        try {
            Jwt jwt = decoder.decode(idToken);
            String email = jwt.getClaimAsString("email");
            if (email == null || email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Google fara email");
            }
            return new GoogleUser(
                    jwt.getSubject(),
                    email,
                    jwt.getClaimAsString("name"),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")));
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Google invalid");
        }
    }
}
