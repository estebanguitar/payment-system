package com.example.paymentsystem.worker;

import com.example.paymentsystem.PaymentSystemApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/** API 웹 요청과 분리되어 Outbox 후속 처리만 독립 실행하는 Worker 진입점이다. */
@EnableScheduling
@SpringBootApplication
@EntityScan("com.example.paymentsystem")
@EnableJpaRepositories("com.example.paymentsystem")
@ComponentScan(
        basePackages = "com.example.paymentsystem",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PaymentSystemApplication.class),
                @ComponentScan.Filter(type = FilterType.REGEX,
                        pattern = "com\\.example\\.paymentsystem\\..*\\.presentation\\..*")
        })
public class OutboxWorkerApplication {

    /** 독립 Worker 애플리케이션을 시작한다. */
    public static void main(String[] args) {
        SpringApplication.run(OutboxWorkerApplication.class, args);
    }
}
