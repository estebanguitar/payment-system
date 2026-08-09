package com.example.paymentsystem.repository.pg;

import com.example.paymentsystem.domain.pg.PgResponseLog;
import com.example.paymentsystem.domain.pg.PgOperationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 암호화된 PG 응답 로그를 결제별로 저장하고 조회한다. */
public interface PgResponseLogRepository extends JpaRepository<PgResponseLog, Long> {

    /** 결제의 PG 응답 로그를 수신 순서대로 조회한다. */
    List<PgResponseLog> findAllByPaymentIdOrderByReceivedAtAsc(Long paymentId);

    /** 결제 ID와 PG 작업 유형으로 응답 로그를 수신 순서대로 조회한다. */
    List<PgResponseLog> findAllByPaymentIdAndOperationTypeOrderByReceivedAtAsc(
            Long paymentId, PgOperationType operationType);
}
