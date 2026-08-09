package com.example.paymentsystem.infrastructure.security;

/** 키·평문·암호문을 노출하지 않고 암복호화 실패를 전달한다. */
public class EncryptionException extends RuntimeException {

    /** 안전한 표준 메시지로 암복호화 오류를 생성한다. */
    public EncryptionException(String message) {
        super(message);
    }

    /** 내부 원인을 보존하되 민감 데이터가 없는 메시지로 오류를 생성한다. */
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
