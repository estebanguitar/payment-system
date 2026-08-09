package com.example.paymentsystem.reconciliation.infrastructure.repository;
import com.example.paymentsystem.reconciliation.domain.ReconciliationCase; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
/** 불일치 Case를 저장하고 안정 키로 조회한다. */ public interface ReconciliationCaseRepository extends JpaRepository<ReconciliationCase,Long>{Optional<ReconciliationCase> findByCaseKey(String caseKey);}
