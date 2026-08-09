package com.example.paymentsystem.worker;

import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** 최근 heartbeat를 기준으로 Worker 스케줄 실행 상태를 진단한다. */
@Component
@RequiredArgsConstructor
public class OutboxWorkerHealthIndicator implements HealthIndicator {

    private final OutboxWorkerMetrics metrics;
    private final Clock clock;

    /** heartbeat가 3분 이상 지연되면 DOWN 상태를 반환한다. */
    @Override
    public Health health() {
        Duration age = Duration.between(metrics.lastHeartbeat(), clock.instant());
        return age.compareTo(Duration.ofMinutes(3)) <= 0
                ? Health.up().withDetail("heartbeatAgeSeconds", age.toSeconds()).build()
                : Health.down().withDetail("heartbeatAgeSeconds", age.toSeconds()).build();
    }
}
