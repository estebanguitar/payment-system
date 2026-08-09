package com.example.paymentsystem.reconciliation.domain;

import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.RunStatus;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.TriggerType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 검사 범위와 실행 결과 통계를 보관하는 대사 실행 원장이다. */
@Getter @Entity @Table(name = "reconciliation_run") @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReconciliationRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="run_key", nullable=false, unique=true, length=160) private String runKey;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private RunStatus status;
    @Column(name="range_start", nullable=false) private LocalDateTime rangeStart;
    @Column(name="range_end", nullable=false) private LocalDateTime rangeEnd;
    @Column(name="started_at", nullable=false) private LocalDateTime startedAt;
    @Column(name="completed_at") private LocalDateTime completedAt;
    @Column(name="checked_count", nullable=false) private long checkedCount;
    @Column(name="mismatch_count", nullable=false) private long mismatchCount;
    @Column(name="failed_count", nullable=false) private long failedCount;
    @Enumerated(EnumType.STRING) @Column(name="trigger_type", nullable=false) private TriggerType triggerType;
    @Column(name="requested_by", length=64) private String requestedBy;

    /** 검증된 범위의 실행을 RUNNING 상태로 생성한다. */
    public static ReconciliationRun start(String key, LocalDateTime from, LocalDateTime to, TriggerType trigger, String requester, LocalDateTime now) {
        if (key == null || key.isBlank() || from == null || to == null || !from.isBefore(to) || trigger == null || now == null) throw new IllegalArgumentException("유효한 대사 실행 정보가 필요합니다.");
        ReconciliationRun run = new ReconciliationRun(); run.runKey=key; run.rangeStart=from; run.rangeEnd=to; run.triggerType=trigger; run.requestedBy=requester; run.startedAt=now; run.status=RunStatus.RUNNING; return run;
    }
    /** 검사 통계를 반영하고 부분 실패 여부에 따라 실행을 완료한다. */
    public void complete(long checked, long mismatches, long failures, LocalDateTime now) { this.checkedCount=checked; this.mismatchCount=mismatches; this.failedCount=failures; this.completedAt=now; this.status=failures == 0 ? RunStatus.COMPLETED : RunStatus.PARTIALLY_FAILED; }
    /** 실행 자체를 계속할 수 없는 경우 실패로 종료한다. */
    public void fail(LocalDateTime now) { this.completedAt=now; this.status=RunStatus.FAILED; }
}
