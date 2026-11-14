package com.hiddengems.repository;

import com.hiddengems.entity.AuthActionPurpose;
import com.hiddengems.entity.AuthActionToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthActionTokenRepository extends JpaRepository<AuthActionToken, Long> {

    Optional<AuthActionToken> findByTokenHashAndPurposeAndUsedAtIsNull(String tokenHash, AuthActionPurpose purpose);

    Optional<AuthActionToken> findTopByUserIdAndPurposeOrderByCreatedAtDesc(Long userId, AuthActionPurpose purpose);

    void deleteByUserIdAndPurposeAndUsedAtIsNull(Long userId, AuthActionPurpose purpose);
}
