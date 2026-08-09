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

/** 결제·지갑·취소·PG·Outbox 원장을 SQL 집계로 교차 검증한다. */
@Component
public class LedgerConsistencyDetector implements ReconciliationDetector {
  private final JdbcTemplate jdbc;

  public LedgerConsistencyDetector(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** 검사기 식별자를 반환한다. */
  public String name() {
    return "ledger-consistency";
  }

  /** 시간 범위의 핵심 원장 불일치를 읽기 전용 쿼리로 탐지한다. */
  public List<ReconciliationFinding> detect(LocalDateTime from, LocalDateTime to) {
    List<ReconciliationFinding> result = new ArrayList<>();
    query(
        result,
        "SELECT w.id,w.customer_id,w.balance,COALESCE(SUM(CASE WHEN wt.transaction_type IN"
            + " ('TOP_UP','REFUND') THEN wt.amount ELSE -wt.amount END),0) expected FROM wallet w"
            + " LEFT JOIN wallet_transaction wt ON wt.wallet_id=w.id GROUP BY"
            + " w.id,w.customer_id,w.balance HAVING w.balance<>COALESCE(SUM(CASE WHEN"
            + " wt.transaction_type IN ('TOP_UP','REFUND') THEN wt.amount ELSE -wt.amount END),0)",
        MismatchType.WALLET_BALANCE_MISMATCH,
        Severity.CRITICAL,
        "wallet",
        null);
    query(
        result,
        "SELECT p.id,p.customer_id,p.amount,COALESCE(SUM(wt.amount),0) expected FROM payment p LEFT"
            + " JOIN wallet_transaction wt ON wt.payment_id=p.id AND wt.transaction_type='PAYMENT'"
            + " WHERE p.status IN ('COMPLETED','PARTIALLY_CANCELED','CANCELED') AND p.created_at>=?"
            + " AND p.created_at<? GROUP BY p.id,p.customer_id,p.amount HAVING"
            + " p.amount<>COALESCE(SUM(wt.amount),0)",
        MismatchType.PAYMENT_DEBIT_AMOUNT_MISMATCH,
        Severity.CRITICAL,
        "payment",
        new Object[] {from, to});
    query(
        result,
        "SELECT p.id,p.customer_id,p.accumulated_cancel_amount,COALESCE(SUM(pc.amount),0) expected"
            + " FROM payment p LEFT JOIN payment_cancel pc ON pc.payment_id=p.id AND"
            + " pc.status='COMPLETED' WHERE p.created_at>=? AND p.created_at<? GROUP BY"
            + " p.id,p.customer_id,p.accumulated_cancel_amount HAVING"
            + " p.accumulated_cancel_amount<>COALESCE(SUM(pc.amount),0)",
        MismatchType.CANCEL_ACCUMULATION_MISMATCH,
        Severity.CRITICAL,
        "payment",
        new Object[] {from, to});
    query(
        result,
        "SELECT p.id,p.customer_id,p.amount,0 expected FROM payment p LEFT JOIN pg_response_log pg"
            + " ON pg.payment_id=p.id WHERE p.status IN"
            + " ('COMPLETED','PARTIALLY_CANCELED','CANCELED') AND p.created_at>=? AND"
            + " p.created_at<? AND pg.id IS NULL",
        MismatchType.PG_RESPONSE_LOG_MISSING,
        Severity.MEDIUM,
        "payment",
        new Object[] {from, to});
    return result;
  }

  private void query(
      List<ReconciliationFinding> out,
      String sql,
      MismatchType type,
      Severity severity,
      String entity,
      Object[] args) {
    Object[] params = args == null ? new Object[0] : args;
    out.addAll(
        jdbc.query(
            sql,
            (rs, n) -> {
              Long id = rs.getLong("id");
              String customer = rs.getString("customer_id");
              String actual =
                  String.valueOf(
                      rs.getLong(
                          entity.equals("wallet")
                              ? "balance"
                              : entity.equals("payment")
                                      && type == MismatchType.CANCEL_ACCUMULATION_MISMATCH
                                  ? "accumulated_cancel_amount"
                                  : "amount"));
              String expected = String.valueOf(rs.getLong("expected"));
              return new ReconciliationFinding(
                  type + ":" + entity + ":" + id,
                  type,
                  severity,
                  customer,
                  entity.equals("wallet") ? id : null,
                  entity.equals("payment") ? id : null,
                  null,
                  null,
                  expected,
                  actual,
                  "{\"version\":1,\"source\":\"" + entity + "\"}");
            },
            params));
  }
}
