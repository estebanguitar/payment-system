package com.example.paymentsystem.common.config;

import com.example.paymentsystem.integration.pg.security.PayloadEncryptor;
import com.example.paymentsystem.scheduler.outbox.OutboxProperties;
import com.example.paymentsystem.integration.pg.PgProperties;
import com.example.paymentsystem.integration.pg.AesGcmPayloadEncryptor;
import com.example.paymentsystem.integration.pg.PaymentSecurityProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** 인프라 설정값을 활성화하고 시간·암호화 구현을 Spring Bean으로 제공한다. */
@Configuration
@EnableConfigurationProperties({PgProperties.class, PaymentSecurityProperties.class, OutboxProperties.class})
public class InfrastructureConfiguration {

    /** LocalDateTime 기반 원장과 동일한 실행 환경 시간대의 시스템 시계를 제공한다. */
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    /** 암호화 키가 주입된 환경에서만 AES-GCM 암호화 구현을 생성한다. */
    @Bean
    @Conditional(EncryptionKeyConfiguredCondition.class)
    public PayloadEncryptor payloadEncryptor(PaymentSecurityProperties properties) {
        return new AesGcmPayloadEncryptor(properties.encryptionKey());
    }

    /** 빈 기본값으로 애플리케이션을 실행할 때 암호화 Bean 생성을 건너뛴다. */
    static class EncryptionKeyConfiguredCondition implements Condition {

        /** 환경 변수로 해석된 키가 공백이 아닐 때만 암호화 Bean을 활성화한다. */
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String key = context.getEnvironment().getProperty("payment.security.encryption-key");
            return key != null && !key.isBlank();
        }
    }
}
