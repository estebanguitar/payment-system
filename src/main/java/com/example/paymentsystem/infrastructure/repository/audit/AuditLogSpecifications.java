package com.example.paymentsystem.infrastructure.repository.audit;

import com.example.paymentsystem.application.dto.audit.AuditLogSearchCondition;
import com.example.paymentsystem.domain.entity.audit.AuditLog;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/** 값이 존재하는 감사 검색 조건을 안전하게 조합한다. */
public final class AuditLogSpecifications {
    private AuditLogSpecifications() {
    }

    /** 기간·클라이언트·경로·상태·추적 ID 조건을 AND로 결합한다. */
    public static Specification<AuditLog> withCondition(AuditLogSearchCondition condition) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (condition.getStartDate() != null) predicates.add(builder.greaterThanOrEqualTo(
                    root.get("requestedAt"), condition.getStartDate()));
            if (condition.getEndDate() != null) predicates.add(builder.lessThanOrEqualTo(
                    root.get("requestedAt"), condition.getEndDate()));
            if (hasText(condition.getClientId())) predicates.add(builder.equal(root.get("clientId"), condition.getClientId()));
            if (hasText(condition.getRequestPath())) predicates.add(builder.equal(root.get("requestPath"), condition.getRequestPath()));
            if (condition.getResponseStatus() != null) predicates.add(builder.equal(root.get("responseStatus"), condition.getResponseStatus()));
            if (hasText(condition.getTraceId())) predicates.add(builder.equal(root.get("traceId"), condition.getTraceId()));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
