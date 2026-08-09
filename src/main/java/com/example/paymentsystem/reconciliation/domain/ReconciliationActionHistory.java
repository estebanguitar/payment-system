package com.example.paymentsystem.reconciliation.domain;

import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.ActionType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.CaseStatus;
import com.example.paymentsystem.shared.domain.exception.DomainErrorCode;
import com.example.paymentsystem.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 원장을 변경하지 않는 운영 판정과 외부 조치 참조를 감사 가능하게 기록한다. */
@Getter
@Entity
@Table(name = "reconciliation_action_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReconciliationActionHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "case_id", nullable = false)
  private Long caseId;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false)
  private ActionType actionType;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", nullable = false)
  private CaseStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false)
  private CaseStatus toStatus;

  @Column(name = "operator_id", nullable = false)
  private String operatorId;

  @Column(nullable = false, length = 500)
  private String reason;

  @Column(name = "external_reference")
  private String externalReference;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  /** 상태 전이와 사유를 불변 이력으로 생성한다. */
  public static ReconciliationActionHistory create(
      Long caseId,
      ActionType type,
      CaseStatus from,
      CaseStatus to,
      String operator,
      String reason,
      String reference,
      LocalDateTime now) {
    if (operator == null || operator.isBlank() || reason == null || reason.isBlank())
      throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, "운영자와 사유는 필수입니다.");
    ReconciliationActionHistory h = new ReconciliationActionHistory();
    h.caseId = caseId;
    h.actionType = type;
    h.fromStatus = from;
    h.toStatus = to;
    h.operatorId = operator;
    h.reason = reason;
    h.externalReference = reference;
    h.createdAt = now;
    return h;
  }
}
