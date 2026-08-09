package com.example.paymentsystem.infrastructure.repository.audit;

import com.example.paymentsystem.domain.entity.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** 감사 로그의 추가 저장과 운영 검색만 제공한다. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
}
