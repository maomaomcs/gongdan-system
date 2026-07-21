package com.school.ticket.repository;

import com.school.ticket.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    long deleteByUsernameAndRole(String username, String role);
    long deleteByExpiresAtBefore(LocalDateTime time);
}
