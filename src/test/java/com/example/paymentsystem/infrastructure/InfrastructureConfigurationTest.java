package com.example.paymentsystem.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.paymentsystem.pg.application.port.out.security.PayloadEncryptor;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** 인프라 암호화 설정의 조건부 Bean 활성화를 검증한다. */
class InfrastructureConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(InfrastructureConfiguration.class);

    /** 키 설정이 없으면 암호화 Bean을 만들지 않아 기본 개발 컨텍스트가 기동되는지 확인한다. */
    @Test
    void skipEncryptorWithoutKey() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(PayloadEncryptor.class));
    }

    /** 올바른 Base64 32바이트 키가 있으면 AES-GCM 구현을 Bean으로 제공하는지 확인한다. */
    @Test
    void createEncryptorWithConfiguredKey() {
        String key = Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

        contextRunner.withPropertyValues("payment.security.encryption-key=" + key)
                .run(context -> assertThat(context).hasSingleBean(PayloadEncryptor.class));
    }
}
