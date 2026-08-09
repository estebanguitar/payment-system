package com.example.paymentsystem.common.audit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** 감사 입력값의 허용 목록과 민감 경로 차단을 검증한다. */
class AuditValueSanitizerTest {
    private final AuditValueSanitizer sanitizer = new AuditValueSanitizer();

    /** 잘못된 클라이언트 값은 anonymous로 대체하는지 검증한다. */
    @Test
    void invalidClientIdBecomesAnonymous() {
        Assertions.assertThat(sanitizer.clientId("client id with spaces")).isEqualTo("anonymous");
    }

    /** 매핑되지 않은 실제 URI 대신 고정 경로를 저장하는지 검증한다. */
    @Test
    void unmappedPathDoesNotExposeActualValue() {
        Assertions.assertThat(sanitizer.requestPath(null)).isEqualTo("/unmapped");
    }
}
