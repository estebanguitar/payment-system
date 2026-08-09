package com.example.paymentsystem.reconciliation.domain;

import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.ActionType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.CaseStatus;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.MismatchType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.Severity;
import jakarta.persistence.Column; import jakarta.persistence.Entity; import jakarta.persistence.EnumType; import jakarta.persistence.Enumerated; import jakarta.persistence.GeneratedValue; import jakarta.persistence.GenerationType; import jakarta.persistence.Id; import jakarta.persistence.Lob; import jakarta.persistence.Table; import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 반복 탐지되는 동일 불일치와 운영 판정 상태를 관리한다. */
@Getter @Entity @Table(name="reconciliation_case") @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class ReconciliationCase {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="case_key",nullable=false,unique=true,length=200) private String caseKey;
 @Column(name="latest_run_id",nullable=false) private Long latestRunId;
 @Enumerated(EnumType.STRING) @Column(name="mismatch_type",nullable=false) private MismatchType mismatchType;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Severity severity;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private CaseStatus status;
 @Column(name="customer_id") private String customerId; @Column(name="wallet_id") private Long walletId; @Column(name="payment_id") private Long paymentId; @Column(name="cancel_id") private Long cancelId; @Column(name="outbox_id") private Long outboxId;
 @Column(name="expected_value") private String expectedValue; @Column(name="actual_value") private String actualValue;
 @Lob @Column(nullable=false) private String evidence;
 @Column(name="first_detected_at",nullable=false) private LocalDateTime firstDetectedAt; @Column(name="last_detected_at",nullable=false) private LocalDateTime lastDetectedAt; @Column(name="resolved_at") private LocalDateTime resolvedAt;
 @Version private long version;
 /** 새 불일치 Case를 OPEN 상태로 생성한다. */
 public static ReconciliationCase open(ReconciliationFinding f, Long runId, LocalDateTime now) { ReconciliationCase c=new ReconciliationCase(); c.caseKey=f.caseKey(); c.latestRunId=runId; c.mismatchType=f.type(); c.severity=f.severity(); c.status=CaseStatus.OPEN; c.customerId=f.customerId(); c.walletId=f.walletId(); c.paymentId=f.paymentId(); c.cancelId=f.cancelId(); c.outboxId=f.outboxId(); c.expectedValue=f.expectedValue(); c.actualValue=f.actualValue(); c.evidence=f.evidence(); c.firstDetectedAt=now; c.lastDetectedAt=now; return c; }
 /** 재탐지 근거와 실행을 갱신하되 운영 판정을 임의로 되돌리지 않는다. */
 public void redetect(ReconciliationFinding f, Long runId, LocalDateTime now) { latestRunId=runId; expectedValue=f.expectedValue(); actualValue=f.actualValue(); evidence=f.evidence(); lastDetectedAt=now; if(status==CaseStatus.RESOLVED) status=CaseStatus.ACTION_REQUIRED; resolvedAt=null; }
 /** 허용된 수동 조치를 적용하고 변경 전 상태를 반환한다. */
 public CaseStatus apply(ActionType action) { CaseStatus before=status; status=switch(action){case START_INVESTIGATION->CaseStatus.INVESTIGATING; case REQUEST_PG_CONFIRMATION,REQUEST_MANUAL_LEDGER_REVIEW,RECHECK->CaseStatus.ACTION_REQUIRED; case MARK_FALSE_POSITIVE->CaseStatus.FALSE_POSITIVE; case CONFIRM_RESOLVED->throw new IllegalArgumentException("해결은 재검증으로만 확정할 수 있습니다.");}; return before; }
 /** 동일 검사기의 정상 재검증 결과로만 해결 처리한다. */
 public void resolve(LocalDateTime now) { status=CaseStatus.RESOLVED; resolvedAt=now; }
}
