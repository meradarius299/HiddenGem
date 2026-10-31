package com.hiddengems.security;

import com.hiddengems.entity.User;
import com.hiddengems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizator inexistent: " + username));

        // Conturile Google nu au parola: "" nu se potriveste niciodata la login cu parola.
        String password = user.getPasswordHash() == null ? "" : user.getPasswordHash();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(password)
                .authorities(user.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority(r.getName().name()))
                        .toList())
                .disabled(!user.isEnabled())
                .build();
    }
}
