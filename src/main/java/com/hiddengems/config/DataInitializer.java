package com.hiddengems.config;

import com.hiddengems.entity.Role;
import com.hiddengems.entity.RoleName;
import com.hiddengems.entity.User;
import com.hiddengems.repository.RoleRepository;
import com.hiddengems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/** La pornire creeaza rolurile si contul de admin (din application.properties) daca nu exista. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        Role userRole = findOrCreateRole(RoleName.ROLE_USER);
        Role adminRole = findOrCreateRole(RoleName.ROLE_ADMIN);

        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRoles(new HashSet<>(Set.of(userRole, adminRole)));
            userRepository.save(admin);
            log.info("Cont admin creat: {}", adminUsername);
        }
    }

    private Role findOrCreateRole(RoleName name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(null, name)));
    }
}
