package com.example.paymentsystem.audit.application;

import com.example.paymentsystem.audit.application.dto.AuditLogCommand;
import com.example.paymentsystem.audit.application.dto.AuditLogResult;
import com.example.paymentsystem.audit.application.dto.AuditLogSearchCondition;
import com.example.paymentsystem.shared.application.dto.PageResult;
import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.shared.application.exception.ApplicationException;
import com.example.paymentsystem.audit.domain.AuditLog;
import com.example.paymentsystem.audit.infrastructure.repository.AuditLogRepository;
import com.example.paymentsystem.audit.infrastructure.repository.AuditLogSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 업무 트랜잭션과 독립된 감사 저장 및 운영 조회를 제공한다. */
@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository repository;

    /** 요청 결과를 별도 트랜잭션으로 추가 저장한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogResult record(AuditLogCommand command) {
        AuditLog log = AuditLog.create(command.traceId(), command.clientId(), command.httpMethod(),
                command.requestPath(), command.responseStatus(), command.errorCode(), command.durationMs(),
                command.requestedAt(), command.completedAt());
        return AuditLogResult.from(repository.save(log));
    }

    /** 허용된 조건으로 감사 로그를 최신순 조회한다. */
    @Transactional(readOnly = true)
    public PageResult<AuditLogResult> search(AuditLogSearchCondition condition) {
        validate(condition);
        return PageResult.from(repository.findAll(AuditLogSpecifications.withCondition(condition),
                PageRequest.of(condition.getPage() - 1, condition.getSize(),
                        Sort.by(Sort.Order.desc("requestedAt"), Sort.Order.desc("id"))))
                .map(AuditLogResult::from));
    }

    /** 감사 로그 식별자로 안전한 단건 정보를 조회한다. */
    @Transactional(readOnly = true)
    public AuditLogResult get(Long id) {
        return repository.findById(id).map(AuditLogResult::from)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.AUDIT_LOG_NOT_FOUND, id));
    }

    private static void validate(AuditLogSearchCondition condition) {
        if (condition == null || condition.getPage() < 1 || condition.getSize() < 1 || condition.getSize() > 100
                || condition.getStartDate() != null && condition.getEndDate() != null
                && condition.getStartDate().isAfter(condition.getEndDate())) {
            throw new ApplicationException(ApplicationErrorCode.INVALID_AUDIT_SEARCH_CONDITION);
        }
    }
}
