package com.hiddengems.service.impl;

import com.hiddengems.dto.AuthResponse;
import com.hiddengems.dto.GoogleLoginRequest;
import com.hiddengems.dto.LoginRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username-ul este deja folosit");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email-ul este deja folosit");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(new HashSet<>(Set.of(defaultRole())));
        userRepository.save(user);

        return login(new LoginRequest(request.username(), request.password()));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username sau parola gresite");
        }
        return toResponse((UserDetails) auth.getPrincipal());
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUser google = googleTokenVerifier.verify(request.idToken());
        if (!google.emailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Emailul Google nu este verificat");
        }

        // 1) cont deja legat de Google  2) cont existent cu acelasi email (il legam)  3) cont nou
        User user = userRepository.findByGoogleId(google.subject())
                .or(() -> userRepository.findByEmail(google.email()).map(existing -> {
                    existing.setGoogleId(google.subject());
                    return existing;
                }))
                .orElseGet(() -> createGoogleUser(google));

        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contul este blocat");
        }
        return toResponse(userDetailsService.loadUserByUsername(user.getUsername()));
    }

    private User createGoogleUser(GoogleUser google) {
        User user = new User();
        user.setUsername(uniqueUsername(google.email()));
        user.setEmail(google.email());
        user.setProvider(AuthProvider.GOOGLE);
        user.setGoogleId(google.subject());
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
                        HttpStatus.INTERNAL_SERVER_ERROR, "Rolul ROLE_USER lipseste din baza de date"));
    }

    private AuthResponse toResponse(UserDetails user) {
        List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return new AuthResponse(jwtService.generateToken(user), user.getUsername(), roles);
    }
}
