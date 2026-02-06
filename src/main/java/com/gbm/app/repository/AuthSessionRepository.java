package com.gbm.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gbm.app.entity.AuthSession;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByToken(String token);
}
