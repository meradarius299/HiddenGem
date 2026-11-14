package com.hiddengems.service.impl;

import com.hiddengems.dto.AuthResponse;
import com.hiddengems.dto.GoogleLoginRequest;
import com.hiddengems.dto.LoginRequest;
import com.hiddengems.dto.MessageResponse;
import com.hiddengems.dto.RegisterRequest;
import com.hiddengems.entity.AuthProvider;
import com.hiddengems.entity.Role;
import com.hiddengems.entity.RoleName;
import com.hiddengems.entity.User;
import com.hiddengems.repository.RoleRepository;
import com.hiddengems.repository.UserRepository;
import com.hiddengems.security.CustomUserDetailsService;
import com.hiddengems.security.GoogleTokenVerifier;
import com.hiddengems.security.GoogleTokenVerifier.GoogleUser;
import com.hiddengems.security.JwtService;
import com.hiddengems.service.AuthService;
import com.hiddengems.service.AccountEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AccountEmailService accountEmailService;

    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already used");
        }
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmailVerified(false);
        user.setRoles(new HashSet<>(Set.of(defaultRole())));
        userRepository.save(user);
        try {
            accountEmailService.sendVerification(user);
        } catch (MailException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Account email could not be sent. Try again shortly.", exception);
        }
        return new MessageResponse("Account created. Check your email for the verification link.");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        userRepository.findByUsername(request.username())
                .filter(user -> user.getProvider() == AuthProvider.LOCAL && !user.isEmailVerified())
                .ifPresent(user -> {
                    if (passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                HttpStatus.FORBIDDEN, "Verify your email before signing in.");
                    }
                });
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password incorrect");
        }
        return toResponse((UserDetails) auth.getPrincipal());
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUser google = googleTokenVerifier.verify(request.idToken());
        if (!google.emailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Gmail is not verified");
        }

        User user = userRepository.findByGoogleId(google.subject())
                .or(() -> userRepository.findByEmail(google.email()).map(existing -> {
                    existing.setGoogleId(google.subject());
                    existing.setEmailVerified(true);
                    return existing;
                }))
                .orElseGet(() -> createGoogleUser(google));

        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account blocked");
        }
        return toResponse(userDetailsService.loadUserByUsername(user.getUsername()));
    }

    private User createGoogleUser(GoogleUser google) {
        User user = new User();
        user.setUsername(uniqueUsername(google.email()));
        user.setEmail(google.email());
        user.setProvider(AuthProvider.GOOGLE);
        user.setGoogleId(google.subject());
        user.setEmailVerified(true);
        user.setRoles(new HashSet<>(Set.of(defaultRole())));
        return userRepository.save(user);
    }

    private String uniqueUsername(String email) {
        int at = email.indexOf('@');
        String base = (at > 0 ? email.substring(0, at) : email).replaceAll("[^A-Za-z0-9._-]", "");
        if (base.length() < 3) {
            base = "user" + base;
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        String candidate = base;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        return candidate;
    }

    private Role defaultRole() {
        return roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Role ROLE_USER is not exist in data base"));
    }

    private AuthResponse toResponse(UserDetails user) {
        List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return new AuthResponse(jwtService.generateToken(user), user.getUsername(), roles);
    }
}
