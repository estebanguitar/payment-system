package com.example.paymentsystem.presentation.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 결제 시스템의 공개 OpenAPI 문서 메타데이터를 구성한다. */
@Configuration
public class OpenApiConfiguration {

    /** 인증 없는 1페이즈 개발 API의 제목·버전·주의사항을 정의한다. */
    @Bean
    public OpenAPI paymentSystemOpenApi() {
        return new OpenAPI().info(new Info()
                .title("결제 시스템 API")
                .version("v1")
                .description("지갑·결제·취소·조회 API입니다. 1페이즈에서는 인증·인가를 제공하지 않습니다."));
    }
}
