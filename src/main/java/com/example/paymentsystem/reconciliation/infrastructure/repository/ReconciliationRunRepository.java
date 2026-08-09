package com.example.paymentsystem.reconciliation.infrastructure.repository;
import com.example.paymentsystem.reconciliation.domain.ReconciliationRun; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
/** 대사 실행 원장을 저장하고 멱등 키로 조회한다. */ public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun,Long>{Optional<ReconciliationRun> findByRunKey(String runKey);}
