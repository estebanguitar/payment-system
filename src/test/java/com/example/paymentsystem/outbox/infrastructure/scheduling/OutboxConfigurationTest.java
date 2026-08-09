package com.example.paymentsystem.outbox.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;

/** 기본 설정에서 미완성 Application 복구 흐름이 실행되지 않는지 검증한다. */
@SpringBootTest
class OutboxConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    /** API 애플리케이션 Bean에는 주기 실행 메서드가 존재하지 않는지 확인한다. */
    @Test
    void disableSchedulerByDefault() {
        boolean scheduledMethodExists = applicationContext.getBeansOfType(Object.class).values().stream()
                .map(ClassUtils::getUserClass)
                .filter(type -> type.getPackageName().startsWith("com.example.paymentsystem"))
                .anyMatch(type -> !MethodIntrospector.selectMethods(type,
                        (MethodFilter) method -> method.isAnnotationPresent(Scheduled.class)).isEmpty());

        assertThat(scheduledMethodExists).isFalse();
    }
}
