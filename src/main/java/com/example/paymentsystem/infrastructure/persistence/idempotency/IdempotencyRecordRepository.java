package com.example.paymentsystem.infrastructure.persistence.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

/** 시스템 전체 멱등키의 선점 및 기존 처리 결과 조회를 제공한다. */
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
}
