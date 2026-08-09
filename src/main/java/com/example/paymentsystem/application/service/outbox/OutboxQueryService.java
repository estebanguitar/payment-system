package com.example.paymentsystem.application.service.outbox;

import com.example.paymentsystem.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.application.exception.ApplicationException;
import com.example.paymentsystem.domain.entity.outbox.PaymentOutbox;
import com.example.paymentsystem.infrastructure.repository.outbox.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 아웃박스 복구 대상 조회를 외부 호출 흐름과 분리된 읽기 트랜잭션으로 수행한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboxQueryService {
    private final PaymentOutboxRepository outboxRepository;

    /** 아웃박스 한 건을 조회하고 없으면 추적 가능한 시스템 오류로 변환한다. */
    public PaymentOutbox get(Long outboxId) {
        return outboxRepository.findById(outboxId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR));
    }
}
