package com.example.paymentsystem.service.idempotency;

import com.example.paymentsystem.domain.cancellation.CancelType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** 업무 요청의 정규화된 필드를 SHA-256 멱등 지문으로 변환한다. */
@Component
public class IdempotencyFingerprintGenerator {

    /** 고객과 금액을 기준으로 결제 요청 지문을 생성한다. */
    public String payment(String customerId, long amount) {
        return sha256("PAYMENT|" + normalize(customerId) + "|" + amount);
    }

    /** 고객과 금액을 기준으로 지갑 충전 요청 지문을 생성한다. */
    public String topUp(String customerId, long amount) {
        return sha256("WALLET_TOP_UP|" + normalize(customerId) + "|" + amount);
    }

    /** 원 결제, 유형 및 금액을 기준으로 취소 요청 지문을 생성한다. */
    public String cancel(Long paymentId, CancelType cancelType, long amount) {
        return sha256("PAYMENT_CANCEL|" + paymentId + "|" + cancelType.name() + "|" + amount);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
