package com.example.paymentsystem.integration.pg;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 개발 환경 가상 PG의 기본 처리 시나리오를 바인딩한다. */
@ConfigurationProperties(prefix = "payment.pg")
public record PgProperties(PgScenario defaultScenario) {

    /** 시나리오가 생략되면 승인 동작을 기본값으로 사용한다. */
    public PgProperties {
        if (defaultScenario == null) {
            defaultScenario = PgScenario.APPROVED;
        }
    }
}
