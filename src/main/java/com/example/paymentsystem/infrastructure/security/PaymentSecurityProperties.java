package com.example.paymentsystem.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 실행 환경에서 주입되는 Base64 AES-256 키 설정을 바인딩한다. */
@ConfigurationProperties(prefix = "payment.security")
public record PaymentSecurityProperties(String encryptionKey) {
}
