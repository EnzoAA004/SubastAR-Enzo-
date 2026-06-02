package com.subastar.subastar.repository;

import com.subastar.subastar.model.LoginTwoFactorToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoginTwoFactorTokenRepository extends JpaRepository<LoginTwoFactorToken, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LoginTwoFactorToken> findByChallengeId(String challengeId);
    List<LoginTwoFactorToken> findByEmailAndUsedAtIsNullAndInvalidatedAtIsNull(String email);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
