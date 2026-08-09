package com.example.paymentsystem.pg.domain;

import com.example.paymentsystem.shared.domain.exception.DomainErrorCode;
import com.example.paymentsystem.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 암호화된 외부 PG 응답 원문과 조회용 메타데이터를 보관한다. */
@Getter
@Entity
@Table(name = "pg_response_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PgResponseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "pg_transaction_id", length = 128)
    private String pgTransactionId;

    @Column(name = "pg_response_code", length = 32)
    private String pgResponseCode;

    @Lob
    @Column(name = "encrypted_payload", nullable = false)
    private String encryptedPayload;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    private PgResponseLog(Long paymentId, String pgTransactionId, String pgResponseCode,
                          String encryptedPayload, LocalDateTime receivedAt) {
        if (paymentId == null || paymentId <= 0) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, "결제 식별자는 필수입니다.");
        }
        if (encryptedPayload == null || encryptedPayload.isBlank()) {
            throw new DomainException(DomainErrorCode.MISSING_ENCRYPTED_PAYLOAD);
        }
        if (receivedAt == null) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, "수신 시각은 필수입니다.");
        }
        this.paymentId = paymentId;
        this.pgTransactionId = pgTransactionId;
        this.pgResponseCode = pgResponseCode;
        this.encryptedPayload = encryptedPayload;
        this.receivedAt = receivedAt;
    }

    /** 암호화가 완료된 PG 응답을 변경 불가능한 로그로 생성한다. */
    public static PgResponseLog create(Long paymentId, String pgTransactionId,
                                       String pgResponseCode, String encryptedPayload,
                                       LocalDateTime receivedAt) {
        return new PgResponseLog(paymentId, pgTransactionId, pgResponseCode,
                encryptedPayload, receivedAt);
    }
}
