package com.example.paymentsystem.service.audit;

import com.example.paymentsystem.dto.audit.AuditLogCommand;
import com.example.paymentsystem.domain.audit.AuditLog;
import com.example.paymentsystem.repository.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 업무 트랜잭션과 독립적으로 API 요청 이력을 저장한다. */
@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository repository;

    /** 요청 결과를 별도 트랜잭션으로 추가 저장한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLogCommand command) {
        AuditLog log = AuditLog.create(command.traceId(), command.clientId(), command.httpMethod(),
                command.requestPath(), command.responseStatus(), command.errorCode(), command.durationMs(),
                command.requestedAt(), command.completedAt(), command.encryptedPayload(), command.payloadTruncated());
        repository.save(log);
    }
}
