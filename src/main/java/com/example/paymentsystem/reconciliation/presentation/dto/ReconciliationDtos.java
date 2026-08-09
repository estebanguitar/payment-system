package com.example.paymentsystem.reconciliation.presentation.dto;

import com.example.paymentsystem.reconciliation.domain.ReconciliationActionHistory;
import com.example.paymentsystem.reconciliation.domain.ReconciliationCase;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.ActionType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.CaseStatus;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.MismatchType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.RunStatus;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.Severity;
import com.example.paymentsystem.reconciliation.domain.ReconciliationRun;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** 대사 운영 API의 요청·응답 계약을 한 경계에 모은다. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReconciliationDtos {
  /** 수동 대사 실행에 필요한 범위와 요청자 정보다. */
  public record RunRequest(
      @NotNull LocalDateTime rangeStart,
      @NotNull LocalDateTime rangeEnd,
      @NotBlank String requestedBy) {}

  /** 운영자가 불일치 Case에 수행할 조치 정보다. */
  public record ActionRequest(
      @NotNull ActionType actionType,
      @NotBlank String operatorId,
      @NotBlank @Size(max = 500) String reason,
      @Size(max = 200) String externalReference) {}

  /** 불일치 해소 여부를 재검증하기 위한 운영자 요청이다. */
  public record RecheckRequest(
      @NotBlank String operatorId, @NotBlank @Size(max = 500) String reason) {}

  /** 대사 실행 결과와 처리 건수를 전달한다. */
  public record RunResponse(
      Long id,
      String runKey,
      RunStatus status,
      LocalDateTime rangeStart,
      LocalDateTime rangeEnd,
      long checkedCount,
      long mismatchCount,
      long failedCount) {
    /** 도메인 실행 결과를 외부 응답 계약으로 변환한다. */
    public static RunResponse from(ReconciliationRun r) {
      return new RunResponse(
          r.getId(),
          r.getRunKey(),
          r.getStatus(),
          r.getRangeStart(),
          r.getRangeEnd(),
          r.getCheckedCount(),
          r.getMismatchCount(),
          r.getFailedCount());
    }
  }

  /** 불일치 Case의 현재 상태와 근거를 전달한다. */
  public record CaseResponse(
      Long id,
      String caseKey,
      MismatchType mismatchType,
      Severity severity,
      CaseStatus status,
      String customerId,
      Long walletId,
      Long paymentId,
      Long cancelId,
      Long outboxId,
      String expectedValue,
      String actualValue,
      String evidence,
      LocalDateTime firstDetectedAt,
      LocalDateTime lastDetectedAt) {
    /** 도메인 Case를 외부 응답 계약으로 변환한다. */
    public static CaseResponse from(ReconciliationCase c) {
      return new CaseResponse(
          c.getId(),
          c.getCaseKey(),
          c.getMismatchType(),
          c.getSeverity(),
          c.getStatus(),
          c.getCustomerId(),
          c.getWalletId(),
          c.getPaymentId(),
          c.getCancelId(),
          c.getOutboxId(),
          c.getExpectedValue(),
          c.getActualValue(),
          c.getEvidence(),
          c.getFirstDetectedAt(),
          c.getLastDetectedAt());
    }
  }

  /** 운영자 조치 전후 상태와 감사 정보를 전달한다. */
  public record ActionResponse(
      ActionType actionType,
      CaseStatus fromStatus,
      CaseStatus toStatus,
      String operatorId,
      String reason,
      String externalReference,
      LocalDateTime createdAt) {
    /** 조치 이력을 외부 응답 계약으로 변환한다. */
    public static ActionResponse from(ReconciliationActionHistory h) {
      return new ActionResponse(
          h.getActionType(),
          h.getFromStatus(),
          h.getToStatus(),
          h.getOperatorId(),
          h.getReason(),
          h.getExternalReference(),
          h.getCreatedAt());
    }
  }

  /** Case의 현재 상태와 누적 조치 이력을 함께 전달한다. */
  public record CaseDetailResponse(CaseResponse reconciliationCase, List<ActionResponse> actions) {}
}
