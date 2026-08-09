package com.example.paymentsystem.infrastructure.security;

/** 외부 PG 원문을 저장 전 암호화하고 권한 있는 경로에서 복호화하는 계약이다. */
public interface PayloadEncryptor {

    /** UTF-8 평문을 인증된 암호문 포맷으로 변환한다. */
    String encrypt(String plaintext);

    /** 버전이 포함된 인증 암호문을 검증하고 UTF-8 평문으로 복원한다. */
    String decrypt(String encryptedPayload);
}
