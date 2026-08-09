package com.example.paymentsystem.reconciliation.infrastructure.detector;

import com.example.paymentsystem.reconciliation.application.ReconciliationDetector;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.MismatchType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.Severity;
import com.example.paymentsystem.reconciliation.domain.ReconciliationFinding;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 장기 대기 상태와 취소 환불·Outbox 처리 흔적 누락을 탐지한다. */
@Component
public class OperationalTraceDetector implements ReconciliationDetector {
  private final JdbcTemplate jdbc;

  public OperationalTraceDetector(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** 검사기 식별자를 반환한다. */
  public String name() {
    return "operational-trace";
  }

  /** 범위 종료 10분 전까지 완료되지 않은 처리와 환불 누락을 찾는다. */
  public List<ReconciliationFinding> detect(LocalDateTime from, LocalDateTime to) {
    List<ReconciliationFinding> result = new ArrayList<>();
    LocalDateTime cutoff = to.minusMinutes(10);
    result.addAll(
        jdbc.query(
            "SELECT id,customer_id FROM payment WHERE status='PENDING' AND created_at>=? AND"
                + " created_at<?",
            (r, n) ->
                finding(
                    MismatchType.PAYMENT_STUCK_PENDING,
                    "payment",
                    r.getLong("id"),
                    r.getString("customer_id"),
                    null),
            from,
            cutoff));
    result.addAll(
        jdbc.query(
            "SELECT pc.id,pc.payment_id,p.customer_id FROM payment_cancel pc JOIN payment p ON"
                + " p.id=pc.payment_id WHERE pc.status='PENDING' AND pc.created_at>=? AND"
                + " pc.created_at<?",
            (r, n) ->
                new ReconciliationFinding(
                    "CANCEL_STUCK_PENDING:cancel:" + r.getLong("id"),
                    MismatchType.CANCEL_STUCK_PENDING,
                    Severity.HIGH,
                    r.getString("customer_id"),
                    null,
                    r.getLong("payment_id"),
                    r.getLong("id"),
                    null,
                    "terminal",
                    "PENDING",
                    "{\"version\":1,\"source\":\"payment_cancel\"}"),
            from,
            cutoff));
    result.addAll(
        jdbc.query(
            "SELECT po.id,po.payment_id,p.customer_id FROM payment_outbox po JOIN payment p ON"
                + " p.id=po.payment_id WHERE po.status='INIT' AND po.created_at>=? AND"
                + " po.created_at<?",
            (r, n) ->
                new ReconciliationFinding(
                    "OUTBOX_STUCK_INIT:outbox:" + r.getLong("id"),
                    MismatchType.OUTBOX_STUCK_INIT,
                    Severity.HIGH,
                    r.getString("customer_id"),
                    null,
                    r.getLong("payment_id"),
                    null,
                    r.getLong("id"),
                    "PUBLISHED",
                    "INIT",
                    "{\"version\":1,\"source\":\"payment_outbox\"}"),
            from,
            cutoff));
    result.addAll(
        jdbc.query(
            "SELECT pc.id,pc.payment_id,p.customer_id,pc.amount FROM payment_cancel pc JOIN payment"
                + " p ON p.id=pc.payment_id LEFT JOIN wallet_transaction wt ON wt.cancel_id=pc.id"
                + " AND wt.transaction_type='REFUND' WHERE pc.status='COMPLETED' AND"
                + " pc.created_at>=? AND pc.created_at<? GROUP BY"
                + " pc.id,pc.payment_id,p.customer_id,pc.amount HAVING"
                + " COALESCE(SUM(wt.amount),0)<>pc.amount",
            (r, n) ->
                new ReconciliationFinding(
                    "CANCEL_REFUND_AMOUNT_MISMATCH:cancel:" + r.getLong("id"),
                    MismatchType.CANCEL_REFUND_AMOUNT_MISMATCH,
                    Severity.CRITICAL,
                    r.getString("customer_id"),
                    null,
                    r.getLong("payment_id"),
                    r.getLong("id"),
                    null,
                    String.valueOf(r.getLong("amount")),
                    "missing-or-different",
                    "{\"version\":1,\"source\":\"wallet_transaction\"}"),
            from,
            to));
    return result;
  }

  private ReconciliationFinding finding(
      MismatchType type, String entity, Long id, String customer, Long outbox) {
    return new ReconciliationFinding(
        type + ":" + entity + ":" + id,
        type,
        Severity.HIGH,
        customer,
        null,
        id,
        null,
        outbox,
        "terminal",
        "PENDING",
        "{\"version\":1,\"source\":\"" + entity + "\"}");
  }
}
