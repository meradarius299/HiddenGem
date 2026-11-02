package com.hiddengems.repository;

import com.hiddengems.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Cautare dupa o parte din username (pentru adaugarea de prieteni), fara utilizatorul curent. */
    List<User> findTop20ByUsernameContainingIgnoreCaseAndIdNotAndEnabledTrue(String fragment, Long excludedId);
}
