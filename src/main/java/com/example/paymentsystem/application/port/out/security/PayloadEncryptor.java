package com.example.paymentsystem.application.port.out.security;

/** PG 원문 저장을 위한 암호화와 권한 있는 복호화를 제공하는 출력 포트다. */
public interface PayloadEncryptor {

    /** UTF-8 평문을 인증된 암호문으로 변환한다. */
    String encrypt(String plaintext);

    /** 인증된 암호문을 검증하고 UTF-8 평문으로 복원한다. */
    String decrypt(String encryptedPayload);
}
