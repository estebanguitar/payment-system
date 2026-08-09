package com.example.paymentsystem.service.idempotency;

import com.example.paymentsystem.common.exception.ApplicationErrorCode;
import com.example.paymentsystem.common.exception.ApplicationException;
import com.example.paymentsystem.domain.idempotency.IdempotencyRecord;
import com.example.paymentsystem.repository.idempotency.IdempotencyRecordRepository;
import com.example.paymentsystem.domain.idempotency.IdempotencyRequestType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** DB 유일성 충돌 후 기존 글로벌 멱등 요청의 동일성 및 리소스를 판정한다. */
@Service
@RequiredArgsConstructor
public class IdempotencyConflictResolver {

    private final IdempotencyRecordRepository repository;

    /** 기존 요청과 유형·지문이 같으면 연결된 리소스 ID를 반환하고 다르면 충돌 처리한다. */
    @Transactional(readOnly = true)
    public Long resolve(String key, IdempotencyRequestType type, String fingerprint) {
        IdempotencyRecord record = repository.findById(key)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR));
        if (!record.matches(type, fingerprint) || record.getResourceId() == null) {
            throw new ApplicationException(ApplicationErrorCode.IDEMPOTENCY_CONFLICT);
        }
        return record.getResourceId();
    }
}
