package com.example.paymentsystem.repository.audit;

import com.example.paymentsystem.domain.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

/** API 요청 감사 이력을 추가 저장한다. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
