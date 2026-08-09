package com.example.paymentsystem.repository.payment;

import com.example.paymentsystem.dto.payment.query.PaymentSearchCondition;
import com.example.paymentsystem.domain.payment.Payment;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/** 운영자 결제 검색 조건을 안전한 JPA Specification으로 조합한다. */
public final class PaymentSpecifications {
    private PaymentSpecifications() {
    }

    /** 값이 존재하는 검색 조건만 AND로 결합한다. */
    public static Specification<Payment> withCondition(PaymentSearchCondition condition) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (condition.getPaymentId() != null) predicates.add(builder.equal(root.get("id"), condition.getPaymentId()));
            if (condition.getCustomerId() != null && !condition.getCustomerId().isBlank())
                predicates.add(builder.equal(root.get("customerId"), condition.getCustomerId()));
            if (condition.getStatus() != null) predicates.add(builder.equal(root.get("status"), condition.getStatus()));
            if (condition.getFailureReason() != null)
                predicates.add(builder.equal(root.get("failureReason"), condition.getFailureReason()));
            if (condition.getStartDate() != null)
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), condition.getStartDate()));
            if (condition.getEndDate() != null)
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), condition.getEndDate()));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
