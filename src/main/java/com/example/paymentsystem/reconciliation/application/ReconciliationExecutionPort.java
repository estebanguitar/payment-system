package com.example.paymentsystem.reconciliation.application;
import com.example.paymentsystem.reconciliation.domain.ReconciliationRun; import java.time.LocalDateTime;
/** API와 별도 Batch/Worker가 동일한 대사를 실행하기 위한 입력 Port다. */
public interface ReconciliationExecutionPort { ReconciliationRun execute(LocalDateTime from, LocalDateTime to, String requester); }
