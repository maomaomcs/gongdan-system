package com.school.ticket.repository;

import com.school.ticket.entity.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface InviteCodeRepository extends JpaRepository<InviteCode, Long>, JpaSpecificationExecutor<InviteCode> {
    Optional<InviteCode> findByCode(String code);
    boolean existsByCode(String code);
}
